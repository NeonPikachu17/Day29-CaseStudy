package com.ewb.gateway;

import com.ewb.common.dto.AuthRequest;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("Login as Asuna returns valid token, role ROLE_CUSTOMER, and accounts")
    void testLogin_Asuna() throws Exception {
        AuthRequest request = new AuthRequest("asuna");

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("asuna"))
                .andExpect(jsonPath("$.fullName").value("Asuna Yuuki"))
                .andExpect(jsonPath("$.role").value(SecurityConstants.ROLE_CUSTOMER))
                .andExpect(jsonPath("$.accountIds[0]").value("EWB-ASU-1001"))
                .andExpect(jsonPath("$.accountIds[1]").value("EWB-ASU-2001"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("asuna");
        assertThat(jwtUtil.extractRole(token)).isEqualTo(SecurityConstants.ROLE_CUSTOMER);
        assertThat(jwtUtil.extractAccountIds(token)).containsExactly("EWB-ASU-1001", "EWB-ASU-2001");
    }

    @Test
    @DisplayName("Login as Agil returns ROLE_OPERATIONS")
    void testLogin_Agil_Operations() throws Exception {
        AuthRequest request = new AuthRequest("agil");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("agil"))
                .andExpect(jsonPath("$.fullName").value("Andrew Gilbert Mills"))
                .andExpect(jsonPath("$.role").value(SecurityConstants.ROLE_OPERATIONS));
    }

    @Test
    @DisplayName("Login as Sinon returns ROLE_AUDITOR")
    void testLogin_Sinon_Auditor() throws Exception {
        AuthRequest request = new AuthRequest("sinon");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("sinon"))
                .andExpect(jsonPath("$.fullName").value("Shino Asada"))
                .andExpect(jsonPath("$.role").value(SecurityConstants.ROLE_AUDITOR));
    }

    @Test
    @DisplayName("Login with unknown username returns 401 Unauthorized")
    void testLogin_UnknownUser_Unauthorized() throws Exception {
        AuthRequest request = new AuthRequest("unknown_hacker");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Unknown persona: unknown_hacker"));
    }
}
