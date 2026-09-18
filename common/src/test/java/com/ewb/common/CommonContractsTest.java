package com.ewb.common;

import com.ewb.common.dto.*;
import com.ewb.common.model.*;
import com.ewb.common.security.JwtUtil;
import com.ewb.common.security.SecurityConstants;
import com.ewb.common.util.MaskingUtil;
import com.ewb.common.util.MoneyUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommonContractsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtUtil jwtUtil = new JwtUtil();

    @Test
    @DisplayName("Verify contracts enum values")
    void testEnums() {
        assertThat(Frequency.valueOf("MONTHLY")).isEqualTo(Frequency.MONTHLY);
        assertThat(OrderStatus.valueOf("ACTIVE")).isEqualTo(OrderStatus.ACTIVE);
        assertThat(OrderStatus.valueOf("PAUSED")).isEqualTo(OrderStatus.PAUSED);
        assertThat(OrderStatus.valueOf("CANCELLED")).isEqualTo(OrderStatus.CANCELLED);
        assertThat(ExecutionStatus.valueOf("PENDING")).isEqualTo(ExecutionStatus.PENDING);
        assertThat(ExecutionStatus.valueOf("SUCCESS")).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(EntryType.valueOf("DEBIT")).isEqualTo(EntryType.DEBIT);
        assertThat(TransferStatus.valueOf("COMPLETED")).isEqualTo(TransferStatus.COMPLETED);
        assertThat(NotificationStatus.valueOf("DELIVERED")).isEqualTo(NotificationStatus.DELIVERED);
    }

    @Test
    @DisplayName("Verify DTO serialization and deserialization")
    void testDtoSerialization() throws Exception {
        TransferRequest request = new TransferRequest("TRF-001", "EWB-ASU-1001", "EWB-ASU-2001", new BigDecimal("5000.00"), "PHP");
        String json = objectMapper.writeValueAsString(request);
        TransferRequest deserialized = objectMapper.readValue(json, TransferRequest.class);
        assertThat(deserialized.getReference()).isEqualTo("TRF-001");
        assertThat(deserialized.getAmount()).isEqualByComparingTo("5000.00");

        TransferResponse response = TransferResponse.completed(
                "TRF-001", "so-9001:2026-10-25T01:00:00Z", "EWB-ASU-1001", "EWB-ASU-2001",
                new BigDecimal("5000.00"), new BigDecimal("15000.00"), new BigDecimal("6000.00"),
                "LED-D-101", "LED-C-102", "2026-10-25T01:00:02Z"
        );
        String respJson = objectMapper.writeValueAsString(response);
        assertThat(respJson).contains("COMPLETED");
        assertThat(respJson).contains("LED-D-101");
    }

    @Test
    @DisplayName("Verify JWT issuance and claim extraction")
    void testJwtUtil() {
        List<String> accounts = List.of("EWB-ASU-1001", "EWB-ASU-2001");
        String token = jwtUtil.generateToken("asuna", SecurityConstants.ROLE_CUSTOMER, accounts);

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("asuna");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ROLE_CUSTOMER");
        assertThat(jwtUtil.extractAccountIds(token)).containsExactly("EWB-ASU-1001", "EWB-ASU-2001");
    }

    @Test
    @DisplayName("Verify Money and Masking utilities")
    void testUtilities() {
        BigDecimal balance = new BigDecimal("20000.000");
        BigDecimal debit = new BigDecimal("5000.00");
        BigDecimal result = MoneyUtil.subtract(balance, debit);
        assertThat(result).isEqualByComparingTo("15000.00");
        assertThat(MoneyUtil.isPositive(debit)).isTrue();
        assertThat(MoneyUtil.isGreaterThanOrEqual(balance, debit)).isTrue();

        assertThat(MaskingUtil.maskAccount("EWB-ASU-1001")).isEqualTo("EWB-***-1001");
    }
}
