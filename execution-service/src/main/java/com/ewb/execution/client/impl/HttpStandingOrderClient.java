package com.ewb.execution.client.impl;

import com.ewb.common.dto.DueStandingOrderDto;
import com.ewb.common.dto.StandingOrderResponse;
import com.ewb.execution.client.StandingOrderClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Component
@ConditionalOnProperty(name = "ewb.clients.mock", havingValue = "false", matchIfMissing = true)
public class HttpStandingOrderClient implements StandingOrderClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public HttpStandingOrderClient(RestTemplate restTemplate,
                                   @Value("${ewb.services.standing-order.url:http://localhost:8081}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    public List<DueStandingOrderDto> getDueOrders(String cutoffUtc) {
        try {
            String url = baseUrl + "/internal/standing-orders/due?cutoff=" + cutoffUtc;
            ResponseEntity<List<DueStandingOrderDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<DueStandingOrderDto>>() {}
            );
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public StandingOrderResponse getStandingOrder(String standingOrderId) {
        try {
            String url = baseUrl + "/standing-orders/" + standingOrderId;
            return restTemplate.getForObject(url, StandingOrderResponse.class);
        } catch (Exception e) {
            return null;
        }
    }
}
