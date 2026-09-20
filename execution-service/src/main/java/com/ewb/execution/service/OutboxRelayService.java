package com.ewb.execution.service;

import com.ewb.common.dto.NotificationEventDto;
import com.ewb.execution.client.NotificationClient;
import com.ewb.execution.entity.OutboxEvent;
import com.ewb.execution.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class OutboxRelayService {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final NotificationClient notificationClient;
    private final ObjectMapper objectMapper;

    public OutboxRelayService(OutboxEventRepository outboxEventRepository,
                              NotificationClient notificationClient,
                              ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.notificationClient = notificationClient;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void relayPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc("PENDING");
        for (OutboxEvent event : pendingEvents) {
            try {
                NotificationEventDto dto = objectMapper.readValue(event.getPayload(), NotificationEventDto.class);
                notificationClient.sendNotification(dto);
                event.setStatus("SENT");
                event.setSentAt(Instant.now());
                outboxEventRepository.save(event);
                log.info("Successfully relayed outbox event {} for execution {}", event.getId(), event.getExecutionId());
            } catch (Exception e) {
                log.warn("Failed to relay outbox event {}: {}", event.getId(), e.getMessage());
            }
        }
    }
}
