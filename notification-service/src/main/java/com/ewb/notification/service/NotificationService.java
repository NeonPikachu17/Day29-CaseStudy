package com.ewb.notification.service;

import com.ewb.common.dto.NotificationEventDto;
import com.ewb.common.model.NotificationStatus;
import com.ewb.notification.dto.NotificationResponse;
import com.ewb.notification.entity.Notification;
import com.ewb.notification.entity.ProcessedEvent;
import com.ewb.notification.exception.NotificationOutageException;
import com.ewb.notification.repository.NotificationRepository;
import com.ewb.notification.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final ProcessedEventRepository processedEventRepository;
    private final NotificationRepository notificationRepository;
    private final AtomicLong deliveryCounter = new AtomicLong(500);

    public NotificationService(ProcessedEventRepository processedEventRepository,
                               NotificationRepository notificationRepository) {
        this.processedEventRepository = processedEventRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public NotificationResponse processEvent(NotificationEventDto event, boolean simulateOutage) {
        String eventId = event.getEventId();
        log.info("Ingesting notification event: eventId={}, customerId={}, standingOrderId={}",
                eventId, event.getCustomerId(), event.getStandingOrderId());

        // 1. Check Outage Simulation
        if (simulateOutage) {
            log.error("Simulated notification outage triggered for eventId: {}", eventId);
            throw new NotificationOutageException("Simulated notification delivery gateway outage");
        }

        // 2. Event Deduplication Check
        if (eventId != null && processedEventRepository.existsByEventId(eventId)) {
            log.warn("Duplicate event detected, safely skipping eventId: {}", eventId);
            return NotificationResponse.skipped("Duplicate event ID");
        }

        // 3. Mark Event as Processed (Guarded by DB unique constraint as well)
        try {
            ProcessedEvent processedEvent = new ProcessedEvent(eventId, Instant.now());
            processedEventRepository.save(processedEvent);
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent duplicate event detected via DB constraint, skipping eventId: {}", eventId);
            return NotificationResponse.skipped("Duplicate event ID");
        }

        // 4. Record Notification Delivery
        String deliveryId = "notif-" + deliveryCounter.incrementAndGet();
        String message = String.format("Standing order %s (%s) for %s %s - %s",
                event.getStandingOrderId(),
                event.getEventType() != null ? event.getEventType() : "EXECUTION",
                event.getAmount() != null ? event.getAmount().toPlainString() : "0.00",
                event.getCurrency() != null ? event.getCurrency() : "PHP",
                event.getStatus() != null ? event.getStatus() : "COMPLETED");

        Notification notification = new Notification(
                deliveryId,
                eventId,
                event.getExecutionId(),
                event.getStandingOrderId(),
                event.getCustomerId(),
                event.getEventType(),
                NotificationStatus.DELIVERED,
                event.getAmount(),
                event.getCurrency(),
                event.getSourceAccountId(),
                event.getDestinationAccountId(),
                event.getPaymentReference(),
                "IN_APP_SMS",
                message,
                Instant.now()
        );

        notificationRepository.save(notification);
        log.info("Notification successfully recorded: deliveryId={}, eventId={}", deliveryId, eventId);

        return NotificationResponse.delivered(deliveryId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByCustomer(String customerId) {
        return notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional(readOnly = true)
    public Optional<Notification> getNotificationByEventId(String eventId) {
        return notificationRepository.findByEventId(eventId);
    }

    @Transactional(readOnly = true)
    public Optional<Notification> getNotificationByDeliveryId(String deliveryId) {
        return notificationRepository.findByDeliveryId(deliveryId);
    }
}
