package com.ewb.notification;

import com.ewb.common.dto.NotificationEventDto;
import com.ewb.notification.repository.NotificationRepository;
import com.ewb.notification.repository.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        processedEventRepository.deleteAll();
    }

    private NotificationEventDto createSampleEvent(String eventId) {
        NotificationEventDto event = new NotificationEventDto();
        event.setEventId(eventId);
        event.setExecutionId("exec-200");
        event.setStandingOrderId("so-9001");
        event.setCustomerId("asuna");
        event.setEventType("EXECUTION_COMPLETED");
        event.setStatus("SUCCESS");
        event.setAmount(new BigDecimal("5000.00"));
        event.setCurrency("PHP");
        event.setSourceAccountId("EWB-ASU-1001");
        event.setDestinationAccountId("EWB-ASU-2001");
        event.setPaymentReference("TRF-20261025-001");
        event.setTimestamp("2026-10-25T01:00:03Z");
        return event;
    }

    @Test
    @DisplayName("Ingest event returns 202 Accepted on first call")
    void testIngestEvent_Success() throws Exception {
        NotificationEventDto event = createSampleEvent("evt-ctrl-001");

        mockMvc.perform(post("/notifications/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.deliveryId").isNotEmpty());
    }

    @Test
    @DisplayName("Ingest duplicate event returns 200 OK SKIPPED")
    void testIngestEvent_DuplicateSkipped() throws Exception {
        NotificationEventDto event = createSampleEvent("evt-ctrl-002");

        // First call -> 202 Accepted
        mockMvc.perform(post("/notifications/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isAccepted());

        // Second call with same eventId -> 200 OK with SKIPPED
        mockMvc.perform(post("/notifications/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SKIPPED"))
                .andExpect(jsonPath("$.message").value("Duplicate event ID"));
    }

    @Test
    @DisplayName("Ingest event with X-Simulate-Outage returns 500 Internal Server Error")
    void testIngestEvent_SimulateOutage() throws Exception {
        NotificationEventDto event = createSampleEvent("evt-ctrl-003");

        mockMvc.perform(post("/notifications/events")
                        .header("X-Simulate-Outage", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("Simulated notification delivery gateway outage"));
    }
}
