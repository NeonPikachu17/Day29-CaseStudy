package com.ewb.gateway;

import com.ewb.common.security.JwtUtil;
import com.ewb.common.security.SecurityConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityRbacTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("testMissingAuthHeader_Returns401: Protected endpoint requires Authorization header")
    void testMissingAuthHeader_Returns401() throws Exception {
        mockMvc.perform(get("/standing-orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Missing or invalid Authorization header"));
    }

    @Test
    @DisplayName("testInternalEndpoint_RejectedAtGateway: Gateway rejects /internal/** with 403 Forbidden")
    void testInternalEndpoint_RejectedAtGateway() throws Exception {
        mockMvc.perform(get("/internal/standing-orders/due"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Direct access to internal endpoints is forbidden via Gateway"));
    }

    @Test
    @DisplayName("testCustomerAccess_OwnOrdersOnly: Customer forbidden from creating order with non-owned source account")
    void testCustomerAccess_NonOwnedAccount_Forbidden() throws Exception {
        // Asuna owns EWB-ASU-1001 and EWB-ASU-2001
        String asunaToken = jwtUtil.generateToken("asuna", SecurityConstants.ROLE_CUSTOMER,
                List.of("EWB-ASU-1001", "EWB-ASU-2001"));

        // Asuna attempts to create order using Kirito's account EWB-KIR-5001
        Map<String, Object> payload = Map.of(
                "sourceAccountId", "EWB-KIR-5001",
                "destinationAccountId", "EWB-ASU-2001",
                "amount", 5000.00
        );

        mockMvc.perform(post("/standing-orders")
                        .header("Authorization", "Bearer " + asunaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Source account EWB-KIR-5001 does not belong to authenticated customer"));
    }

    @Test
    @DisplayName("testAuditorAccess_ReadOnly: Auditor forbidden from mutating operations (POST, PUT, DELETE)")
    void testAuditorAccess_ReadOnly() throws Exception {
        String sinonToken = jwtUtil.generateToken("sinon", SecurityConstants.ROLE_AUDITOR, Collections.emptyList());

        Map<String, Object> payload = Map.of(
                "sourceAccountId", "EWB-ASU-1001",
                "destinationAccountId", "EWB-ASU-2001",
                "amount", 5000.00
        );

        mockMvc.perform(post("/standing-orders")
                        .header("Authorization", "Bearer " + sinonToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Auditor role has read-only access"));
    }

    @Test
    @DisplayName("testCustomerAccess_RecoverForbidden: Customer cannot call recovery endpoint")
    void testCustomerAccess_RecoverForbidden() throws Exception {
        String asunaToken = jwtUtil.generateToken("asuna", SecurityConstants.ROLE_CUSTOMER,
                List.of("EWB-ASU-1001", "EWB-ASU-2001"));

        mockMvc.perform(post("/executions/101/recover")
                        .header("Authorization", "Bearer " + asunaToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Operations permission required for recovery operations"));
    }
}
