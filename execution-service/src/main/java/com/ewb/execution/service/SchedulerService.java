package com.ewb.execution.service;

import com.ewb.common.dto.*;
import com.ewb.common.model.ExecutionStatus;
import com.ewb.common.model.OrderStatus;
import com.ewb.common.model.TransferStatus;
import com.ewb.execution.client.PaymentClient;
import com.ewb.execution.client.StandingOrderClient;
import com.ewb.execution.entity.Execution;
import com.ewb.execution.entity.ExecutionAttempt;
import com.ewb.execution.entity.OutboxEvent;
import com.ewb.execution.repository.ExecutionAttemptRepository;
import com.ewb.execution.repository.ExecutionRepository;
import com.ewb.execution.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    private final ExecutionRepository executionRepository;
    private final ExecutionAttemptRepository executionAttemptRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final StandingOrderClient standingOrderClient;
    private final PaymentClient paymentClient;
    private final ObjectMapper objectMapper;

    @Value("${ewb.worker.id:worker-1}")
    private String workerId;

    public SchedulerService(ExecutionRepository executionRepository,
                            ExecutionAttemptRepository executionAttemptRepository,
                            OutboxEventRepository outboxEventRepository,
                            StandingOrderClient standingOrderClient,
                            PaymentClient paymentClient,
                            ObjectMapper objectMapper) {
        this.executionRepository = executionRepository;
        this.executionAttemptRepository = executionAttemptRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.standingOrderClient = standingOrderClient;
        this.paymentClient = paymentClient;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    public void pollAndExecute() {
        discoverDueOrders();
        processClaimedOrders();
    }

    public void discoverDueOrders() {
        String nowUtc = Instant.now().toString();
        List<DueStandingOrderDto> dueOrders = standingOrderClient.getDueOrders(nowUtc);
        if (dueOrders == null || dueOrders.isEmpty()) {
            return;
        }

        for (DueStandingOrderDto due : dueOrders) {
            try {
                Execution execution = new Execution();
                execution.setStandingOrderId(due.getStandingOrderId());
                execution.setScheduledOccurrenceUtc(due.getScheduledOccurrenceUtc());
                execution.setCustomerId(due.getCustomerId());
                execution.setSourceAccountId(due.getSourceAccountId());
                execution.setDestinationAccountId(due.getDestinationAccountId());
                execution.setAmount(due.getAmount());
                execution.setCurrency(due.getCurrency());
                execution.setInstructionVersion(due.getVersion());
                execution.setStatus(ExecutionStatus.PENDING);
                executionRepository.save(execution);
                log.info("Discovered due order {} at {}", due.getStandingOrderId(), due.getScheduledOccurrenceUtc());
            } catch (DataIntegrityViolationException e) {
                // Deduplication guarantee: already registered for this occurrence
                log.debug("Execution already exists for order {} at {}", due.getStandingOrderId(), due.getScheduledOccurrenceUtc());
            } catch (Exception e) {
                log.error("Failed to register due order {}: {}", due.getStandingOrderId(), e.getMessage());
            }
        }
    }

    public void processClaimedOrders() {
        List<Execution> pendingOrClaimed = executionRepository.findByStatus(ExecutionStatus.PENDING);
        for (Execution execution : pendingOrClaimed) {
            claimAndProcess(execution.getId());
        }
    }

    @Transactional
    public void claimAndProcess(String executionId) {
        Instant now = Instant.now();
        Instant leaseExpiresAt = now.plus(Duration.ofSeconds(60));

        int claimed = executionRepository.claimExecutionLease(
                executionId,
                workerId,
                leaseExpiresAt,
                now,
                ExecutionStatus.PENDING,
                ExecutionStatus.CLAIMED
        );

        if (claimed == 0) {
            // Already claimed by an active worker or completed
            return;
        }

        Execution execution = executionRepository.findById(executionId).orElse(null);
        if (execution == null) return;

        processExecution(execution);
    }

    public void processExecution(Execution execution) {
        // Pre-Execution Cut-off & Revalidation
        StandingOrderResponse currentOrder = standingOrderClient.getStandingOrder(execution.getStandingOrderId());
        if (currentOrder != null && (currentOrder.getStatus() == OrderStatus.PAUSED || currentOrder.getStatus() == OrderStatus.CANCELLED)) {
            execution.setStatus(ExecutionStatus.CANCELLED);
            execution.setCompletedAt(Instant.now());
            execution.setErrorMessage("Order status was " + currentOrder.getStatus() + " prior to execution");
            executionRepository.save(execution);
            publishOutboxEvent(execution, "EXECUTION_CANCELLED");
            return;
        }

        int nextAttempt = execution.getAttemptsCount() + 1;
        execution.setAttemptsCount(nextAttempt);

        String paymentReference = "TRF-" + execution.getStandingOrderId() + "-" + System.currentTimeMillis();
        execution.setPaymentReference(paymentReference);

        String idempotencyKey = execution.getStandingOrderId() + ":" + execution.getScheduledOccurrenceUtc();

        TransferRequest transferRequest = new TransferRequest(
                paymentReference,
                execution.getSourceAccountId(),
                execution.getDestinationAccountId(),
                execution.getAmount(),
                execution.getCurrency()
        );

        ExecutionAttempt attempt = new ExecutionAttempt();
        attempt.setExecutionId(execution.getId());
        attempt.setAttemptNumber(nextAttempt);
        attempt.setPaymentReference(paymentReference);

        try {
            TransferResponse response = paymentClient.submitTransfer(transferRequest, idempotencyKey);
            if (response != null && response.getStatus() == TransferStatus.COMPLETED) {
                execution.setStatus(ExecutionStatus.SUCCESS);
                execution.setCompletedAt(Instant.now());
                attempt.setStatus("SUCCESS");
                publishOutboxEvent(execution, "EXECUTION_COMPLETED");
            } else {
                execution.setStatus(ExecutionStatus.FAILED);
                execution.setCompletedAt(Instant.now());
                String error = response != null ? response.getErrorCode() + ": " + response.getMessage() : "Unknown error";
                execution.setErrorMessage(error);
                attempt.setStatus("FAILED");
                attempt.setErrorMessage(error);
                publishOutboxEvent(execution, "EXECUTION_FAILED");
            }
        } catch (Exception ex) {
            // Potential 504 / timeout / uncertain outcome -> Recover by Reference
            log.warn("Payment call failed or timed out. Initiating query-by-reference recovery for {}", paymentReference);
            try {
                TransferResponse recovered = paymentClient.getTransferByReference(paymentReference);
                if (recovered != null && recovered.getStatus() == TransferStatus.COMPLETED) {
                    execution.setStatus(ExecutionStatus.SUCCESS);
                    execution.setCompletedAt(Instant.now());
                    attempt.setStatus("SUCCESS_RECOVERED");
                    publishOutboxEvent(execution, "EXECUTION_COMPLETED");
                } else if (recovered != null && recovered.getStatus() == TransferStatus.FAILED) {
                    execution.setStatus(ExecutionStatus.FAILED);
                    execution.setCompletedAt(Instant.now());
                    execution.setErrorMessage(recovered.getMessage());
                    attempt.setStatus("FAILED");
                    attempt.setErrorMessage(recovered.getMessage());
                    publishOutboxEvent(execution, "EXECUTION_FAILED");
                } else {
                    // Permanently unresolved or not found
                    execution.setStatus(ExecutionStatus.UNRESOLVED);
                    execution.setErrorMessage("Timeout occurred and transfer status could not be verified");
                    attempt.setStatus("UNRESOLVED");
                    attempt.setErrorMessage(ex.getMessage());
                }
            } catch (Exception recoveryEx) {
                execution.setStatus(ExecutionStatus.UNRESOLVED);
                execution.setErrorMessage("Timeout occurred and recovery call failed: " + recoveryEx.getMessage());
                attempt.setStatus("UNRESOLVED");
                attempt.setErrorMessage(recoveryEx.getMessage());
            }
        }

        executionAttemptRepository.save(attempt);
        executionRepository.save(execution);
    }

    private void publishOutboxEvent(Execution execution, String eventType) {
        try {
            NotificationEventDto event = new NotificationEventDto();
            event.setEventId("evt-" + UUID.randomUUID());
            event.setExecutionId(execution.getId());
            event.setStandingOrderId(execution.getStandingOrderId());
            event.setCustomerId(execution.getCustomerId());
            event.setEventType(eventType);
            event.setStatus(execution.getStatus().name());
            event.setAmount(execution.getAmount());
            event.setCurrency(execution.getCurrency());
            event.setSourceAccountId(execution.getSourceAccountId());
            event.setDestinationAccountId(execution.getDestinationAccountId());
            event.setPaymentReference(execution.getPaymentReference());
            event.setErrorMessage(execution.getErrorMessage());
            event.setTimestamp(Instant.now().toString());

            OutboxEvent outbox = new OutboxEvent();
            outbox.setExecutionId(execution.getId());
            outbox.setEventType(eventType);
            outbox.setPayload(objectMapper.writeValueAsString(event));
            outbox.setStatus("PENDING");
            outboxEventRepository.save(outbox);
        } catch (Exception e) {
            log.error("Failed to build OutboxEvent for execution {}: {}", execution.getId(), e.getMessage());
        }
    }
}
