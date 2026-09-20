package com.ewb.gateway.filter;

import com.ewb.common.security.JwtUtil;
import com.ewb.common.security.SecurityConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Component
@Order(1)
public class ReverseProxyFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(ReverseProxyFilter.class);

    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "host",
            "content-length",
            "connection",
            "transfer-encoding",
            "upgrade",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer"
    );

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${ewb.routes.standing-order-service:http://localhost:8081}")
    private String standingOrderServiceUrl;

    @Value("${ewb.routes.execution-service:http://localhost:8082}")
    private String executionServiceUrl;

    @Value("${ewb.routes.payment-service:http://localhost:8083}")
    private String paymentServiceUrl;

    @Value("${ewb.routes.notification-service:http://localhost:8084}")
    private String notificationServiceUrl;

    public ReverseProxyFilter() {
        this.jwtUtil = new JwtUtil();
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public ReverseProxyFilter(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void setStandingOrderServiceUrl(String url) {
        this.standingOrderServiceUrl = url;
    }

    public void setExecutionServiceUrl(String url) {
        this.executionServiceUrl = url;
    }

    public void setPaymentServiceUrl(String url) {
        this.paymentServiceUrl = url;
    }

    public void setNotificationServiceUrl(String url) {
        this.notificationServiceUrl = url;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI();
        String method = request.getMethod();

        // 1. Allow public endpoints & CORS preflight
        if (isPublicPath(path) || "OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Reject external traffic to /internal/**
        if (isInternalPath(path)) {
            log.warn("Blocked external attempt to access internal path: {}", path);
            sendJsonError(response, HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "Direct access to internal endpoints is forbidden via Gateway");
            return;
        }

        // 3. Validate Authorization header & JWT
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            sendJsonError(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                    "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7).trim();
        if (!jwtUtil.validateToken(token)) {
            log.warn("JWT token validation failed for path: {}", path);
            sendJsonError(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                    "Invalid or expired JWT token");
            return;
        }

        String userId = jwtUtil.extractUsername(token);
        String role = jwtUtil.extractRole(token);
        List<String> accounts = jwtUtil.extractAccountIds(token);
        if (accounts == null) {
            accounts = Collections.emptyList();
        }

        // 4. Wrap request to allow body inspection for sourceAccountId check
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);

        // 5. RBAC Validation
        // 5a. Auditor is strictly read-only (GET and HEAD only)
        if (SecurityConstants.ROLE_AUDITOR.equalsIgnoreCase(role)) {
            if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
                log.warn("Auditor user {} attempted non-read operation: {} {}", userId, method, path);
                sendJsonError(response, HttpStatus.FORBIDDEN, "FORBIDDEN",
                        "Auditor role has read-only access");
                return;
            }
        }

        // 5b. Customer permissions & source account ownership
        if (SecurityConstants.ROLE_CUSTOMER.equalsIgnoreCase(role)) {
            // Customers cannot perform operational recoveries
            if (path.contains("/recover")) {
                log.warn("Customer user {} attempted ops endpoint: {}", userId, path);
                sendJsonError(response, HttpStatus.FORBIDDEN, "FORBIDDEN",
                        "Operations permission required for recovery operations");
                return;
            }

            // Verify source account ownership on standing order creation
            if ("POST".equalsIgnoreCase(method) && isStandingOrderCreationPath(path)) {
                String body = cachedRequest.getBodyAsString();
                if (body != null && !body.isBlank()) {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(body);
                        JsonNode sourceAccountNode = jsonNode.get("sourceAccountId");
                        if (sourceAccountNode != null && !sourceAccountNode.isNull()) {
                            String sourceAccountId = sourceAccountNode.asText();
                            if (!accounts.contains(sourceAccountId)) {
                                log.warn("Customer {} attempted to create order with non-owned account {}", userId, sourceAccountId);
                                sendJsonError(response, HttpStatus.FORBIDDEN, "FORBIDDEN",
                                        "Source account " + sourceAccountId + " does not belong to authenticated customer");
                                return;
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse request JSON for source account validation: {}", e.getMessage());
                    }
                }
            }
        }

        // 6. Reverse Proxy Dispatch
        dispatchToDownstream(cachedRequest, response, path, method, userId, role, accounts);
    }

    private void dispatchToDownstream(CachedBodyHttpServletRequest request, HttpServletResponse response,
                                      String path, String method, String userId, String role, List<String> accounts)
            throws IOException {

        String targetBaseUrl;
        String rewrittenPath;

        if (path.startsWith("/standing-orders") || path.startsWith("/api/standing-orders")) {
            targetBaseUrl = standingOrderServiceUrl;
            rewrittenPath = path.startsWith("/api/") ? path.substring(4) : path;
        } else if (path.startsWith("/executions") || path.startsWith("/api/executions")) {
            targetBaseUrl = executionServiceUrl;
            rewrittenPath = path.startsWith("/api/") ? path.substring(4) : path;
        } else if (path.startsWith("/transfers") || path.startsWith("/api/transfers")) {
            targetBaseUrl = paymentServiceUrl;
            rewrittenPath = path.startsWith("/api/") ? path.substring(4) : path;
        } else if (path.startsWith("/notifications") || path.startsWith("/api/notifications")) {
            targetBaseUrl = notificationServiceUrl;
            rewrittenPath = path.startsWith("/api/") ? path.substring(4) : path;
        } else {
            sendJsonError(response, HttpStatus.NOT_FOUND, "NOT_FOUND", "No route found for path: " + path);
            return;
        }

        String queryString = request.getQueryString();
        String fullTargetUri = targetBaseUrl + rewrittenPath + (queryString != null ? "?" + queryString : "");

        try {
            HttpRequest.BodyPublisher bodyPublisher = request.getCachedBody().length > 0
                    ? HttpRequest.BodyPublishers.ofByteArray(request.getCachedBody())
                    : HttpRequest.BodyPublishers.noBody();

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(fullTargetUri))
                    .method(method, bodyPublisher)
                    .timeout(Duration.ofSeconds(10));

            // Copy incoming headers
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames != null && headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if (HOP_BY_HOP_HEADERS.contains(headerName.toLowerCase(Locale.ROOT))) {
                    continue;
                }
                Enumeration<String> headerValues = request.getHeaders(headerName);
                while (headerValues.hasMoreElements()) {
                    reqBuilder.header(headerName, headerValues.nextElement());
                }
            }

            // Downstream Forwarded Headers (contracts.md Section 2)
            reqBuilder.header(SecurityConstants.HEADER_USER_ID, userId);
            reqBuilder.header(SecurityConstants.HEADER_USER_ROLE, role);
            reqBuilder.header(SecurityConstants.HEADER_USER_ACCOUNTS, String.join(",", accounts));

            HttpRequest outboundRequest = reqBuilder.build();
            HttpResponse<InputStream> downstreamResponse = httpClient.send(outboundRequest,
                    HttpResponse.BodyHandlers.ofInputStream());

            // Relay response status
            response.setStatus(downstreamResponse.statusCode());

            // Relay response headers
            downstreamResponse.headers().map().forEach((k, values) -> {
                if (!HOP_BY_HOP_HEADERS.contains(k.toLowerCase(Locale.ROOT))) {
                    for (String val : values) {
                        response.addHeader(k, val);
                    }
                }
            });

            // Stream response body
            try (InputStream in = downstreamResponse.body(); OutputStream out = response.getOutputStream()) {
                in.transferTo(out);
                out.flush();
            }

        } catch (ConnectException e) {
            log.warn("Downstream connection failed for target: {}", fullTargetUri);
            sendJsonError(response, HttpStatus.BAD_GATEWAY, "BAD_GATEWAY",
                    "Downstream service unavailable at " + targetBaseUrl);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            sendJsonError(response, HttpStatus.GATEWAY_TIMEOUT, "GATEWAY_TIMEOUT",
                    "Downstream request was interrupted");
        } catch (Exception e) {
            log.error("Proxy error dispatching to {}: {}", fullTargetUri, e.getMessage());
            sendJsonError(response, HttpStatus.BAD_GATEWAY, "BAD_GATEWAY",
                    "Failed to forward request: " + e.getMessage());
        }
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/auth")
                || path.startsWith("/api/auth")
                || path.startsWith("/actuator")
                || path.startsWith("/h2-console")
                || path.equals("/favicon.ico");
    }

    private boolean isInternalPath(String path) {
        return path.startsWith("/internal")
                || path.startsWith("/api/internal");
    }

    private boolean isStandingOrderCreationPath(String path) {
        return path.equals("/standing-orders")
                || path.equals("/api/standing-orders")
                || path.equals("/standing-orders/")
                || path.equals("/api/standing-orders/");
    }

    private void sendJsonError(HttpServletResponse response, HttpStatus status, String errorCode, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> errorPayload = new LinkedHashMap<>();
        errorPayload.put("status", errorCode);
        errorPayload.put("message", message);
        response.getOutputStream().write(objectMapper.writeValueAsBytes(errorPayload));
        response.getOutputStream().flush();
    }
}
