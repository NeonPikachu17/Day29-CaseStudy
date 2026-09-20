package com.ewb.notification.controller;

import com.ewb.common.dto.NotificationEventDto;
import com.ewb.notification.dto.NotificationResponse;
import com.ewb.notification.entity.Notification;
import com.ewb.notification.exception.NotificationOutageException;
import com.ewb.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/notifications", "/api/notifications"})
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/events")
    public ResponseEntity<NotificationResponse> ingestEvent(
            @RequestBody NotificationEventDto event,
            @RequestHeader(value = "X-Simulate-Outage", defaultValue = "false") String simulateOutageHeader) {

        boolean simulateOutage = Boolean.parseBoolean(simulateOutageHeader);
        NotificationResponse response = notificationService.processEvent(event, simulateOutage);

        if ("SKIPPED".equals(response.getStatus())) {
            // Contracts Section 6.2: 200 OK for duplicate event ID
            return ResponseEntity.ok(response);
        }

        // Contracts Section 6.2: 202 Accepted for newly delivered event
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getAllNotifications() {
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Notification>> getNotificationsByCustomer(@PathVariable String customerId) {
        return ResponseEntity.ok(notificationService.getNotificationsByCustomer(customerId));
    }

    @GetMapping("/events/{eventId}")
    public ResponseEntity<Notification> getNotificationByEventId(@PathVariable String eventId) {
        return notificationService.getNotificationByEventId(eventId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{deliveryId}")
    public ResponseEntity<Notification> getNotificationByDeliveryId(@PathVariable String deliveryId) {
        return notificationService.getNotificationByDeliveryId(deliveryId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @ExceptionHandler(NotificationOutageException.class)
    public ResponseEntity<NotificationResponse> handleOutage(NotificationOutageException ex) {
        // Contracts Section 6.2: 500 Internal Server Error when outage simulated
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(NotificationResponse.failed(ex.getMessage()));
    }
}
