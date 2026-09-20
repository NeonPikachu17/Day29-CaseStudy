package com.ewb.notification;

import com.ewb.common.dto.NotificationEventDto;
import com.ewb.common.model.NotificationStatus;
import com.ewb.notification.dto.NotificationResponse;
import com.ewb.notification.entity.Notification;
import com.ewb.notification.exception.NotificationOutageException;
import com.ewb.notification.repository.NotificationRepository;
import com.ewb.notification.repository.ProcessedEventRepository;
import com.ewb.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        processedEventRepository.deleteAll();
    }

    private NotificationEventDto createSampleEvent(String eventId) {
        NotificationEventDto event = new NotificationEventDto();
        event.setEventId(eventId);
        event.setExecutionId("exec-101");
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
    @DisplayName("testEventDeduplication_DuplicateEventId_SkippedSafely: Posting the same event twice results in exactly 1 delivery record")
    void testEventDeduplication_DuplicateEventId_SkippedSafely() {
        NotificationEventDto event = createSampleEvent("evt-dedup-001");

        // First delivery
        NotificationResponse firstResp = notificationService.processEvent(event, false);
        assertThat(firstResp.getStatus()).isEqualTo("DELIVERED");
        assertThat(firstResp.getDeliveryId()).isNotNull();

        // Duplicate delivery with same eventId
        NotificationResponse secondResp = notificationService.processEvent(event, false);
        assertThat(secondResp.getStatus()).isEqualTo("SKIPPED");
        assertThat(secondResp.getMessage()).contains("Duplicate event ID");

        // Verify only 1 delivery record exists in database
        List<Notification> records = notificationRepository.findAll();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getEventId()).isEqualTo("evt-dedup-001");
        assertThat(records.get(0).getStatus()).isEqualTo(NotificationStatus.DELIVERED);
    }

    @Test
    @DisplayName("testNotificationFailure_DoesNotThrowUncaughtException: Simulating delivery failure logs error cleanly without breaking service")
    void testNotificationFailure_DoesNotThrowUncaughtException() {
        NotificationEventDto event = createSampleEvent("evt-outage-001");

        // Simulating outage throws NotificationOutageException cleanly
        assertThatThrownBy(() -> notificationService.processEvent(event, true))
                .isInstanceOf(NotificationOutageException.class)
                .hasMessageContaining("Simulated notification delivery gateway outage");

        // Verify that failed/interrupted event was not saved as delivered
        assertThat(processedEventRepository.existsByEventId("evt-outage-001")).isFalse();
        assertThat(notificationRepository.findByEventId("evt-outage-001")).isEmpty();
    }

    @Test
    @DisplayName("testSuccessfulNotificationDelivery: Verifies full payload mapping and querying")
    void testSuccessfulNotificationDelivery() {
        NotificationEventDto event = createSampleEvent("evt-happy-001");

        NotificationResponse response = notificationService.processEvent(event, false);
        assertThat(response.getStatus()).isEqualTo("DELIVERED");

        List<Notification> customerNotifs = notificationService.getNotificationsByCustomer("asuna");
        assertThat(customerNotifs).hasSize(1);
        Notification notif = customerNotifs.get(0);
        assertThat(notif.getCustomerId()).isEqualTo("asuna");
        assertThat(notif.getAmount()).isEqualByComparingTo("5000.00");
        assertThat(notif.getSourceAccountId()).isEqualTo("EWB-ASU-1001");
        assertThat(notif.getMessage()).contains("so-9001");
    }
}
