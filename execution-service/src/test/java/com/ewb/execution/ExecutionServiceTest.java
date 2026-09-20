package com.ewb.execution;

import com.ewb.common.dto.*;
import com.ewb.common.model.ExecutionStatus;
import com.ewb.common.model.OrderStatus;
import com.ewb.common.model.TransferStatus;
import com.ewb.execution.client.NotificationClient;
import com.ewb.execution.client.PaymentClient;
import com.ewb.execution.client.StandingOrderClient;
import com.ewb.execution.entity.Execution;
import com.ewb.execution.entity.OutboxEvent;
import com.ewb.execution.repository.ExecutionAttemptRepository;
import com.ewb.execution.repository.ExecutionRepository;
import com.ewb.execution.repository.OutboxEventRepository;
import com.ewb.execution.service.SchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "spring.cloud.config.enabled=false",
    "ewb.clients.mock=true"
})
public class ExecutionServiceTest {

    @Autowired
    private ExecutionRepository executionRepository;

    @Autowired
    private ExecutionAttemptRepository executionAttemptRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private SchedulerService schedulerService;

    @MockBean
    private StandingOrderClient standingOrderClient;

    @MockBean
    private PaymentClient paymentClient;

    @MockBean
    private NotificationClient notificationClient;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        executionAttemptRepository.deleteAll();
        executionRepository.deleteAll();
    }

    @Test
    void testDuplicatePrevention_ConcurrentDiscovery_RejectsDuplicate() {
        Execution exec1 = new Execution();
        exec1.setStandingOrderId("so-9001");
        exec1.setScheduledOccurrenceUtc("2026-10-25T01:00:00Z");
        exec1.setStatus(ExecutionStatus.PENDING);
        executionRepository.saveAndFlush(exec1);

        Execution exec2 = new Execution();
        exec2.setStandingOrderId("so-9001");
        exec2.setScheduledOccurrenceUtc("2026-10-25T01:00:00Z");
        exec2.setStatus(ExecutionStatus.PENDING);

        assertThrows(DataIntegrityViolationException.class, () -> {
            executionRepository.saveAndFlush(exec2);
        });
    }

    @Test
    void testLeaseExpiration_CrashedWorker_ReclaimedBySecondWorker() {
        Execution exec = new Execution();
        exec.setStandingOrderId("so-9002");
        exec.setScheduledOccurrenceUtc("2026-10-25T01:00:00Z");
        exec.setStatus(ExecutionStatus.CLAIMED);
        exec.setWorkerId("crashed-worker");
        exec.setLeaseExpiresAt(Instant.now().minus(Duration.ofSeconds(10))); // Expired
        exec = executionRepository.saveAndFlush(exec);

        int updated = executionRepository.claimExecutionLease(
                exec.getId(),
                "new-worker",
                Instant.now().plus(Duration.ofSeconds(60)),
                Instant.now(),
                ExecutionStatus.PENDING,
                ExecutionStatus.CLAIMED
        );

        assertEquals(1, updated);
    }

    @Test
    void testPaymentTimeoutRecovery_QueriesReferenceAndAvoidsDoubleDebit() {
        StandingOrderResponse so = new StandingOrderResponse();
        so.setStatus(OrderStatus.ACTIVE);
        when(standingOrderClient.getStandingOrder(anyString())).thenReturn(so);

        when(paymentClient.submitTransfer(any(), anyString()))
                .thenThrow(new RuntimeException("Simulated HTTP 504 Gateway Timeout"));

        TransferResponse recoveredResponse = TransferResponse.completed(
                "TRF-so-9001-mock", "key", "EWB-ASU-1001", "EWB-ASU-2001",
                BigDecimal.valueOf(5000), BigDecimal.valueOf(15000), BigDecimal.valueOf(6000),
                "LED-D-1", "LED-C-2", Instant.now().toString()
        );
        when(paymentClient.getTransferByReference(anyString())).thenReturn(recoveredResponse);

        Execution exec = new Execution();
        exec.setStandingOrderId("so-9001");
        exec.setScheduledOccurrenceUtc("2026-10-25T01:00:00Z");
        exec.setSourceAccountId("EWB-ASU-1001");
        exec.setDestinationAccountId("EWB-ASU-2001");
        exec.setAmount(BigDecimal.valueOf(5000));
        exec.setCurrency("PHP");
        exec.setStatus(ExecutionStatus.PENDING);
        exec = executionRepository.saveAndFlush(exec);

        schedulerService.processExecution(exec);

        Execution updated = executionRepository.findById(exec.getId()).orElseThrow();
        assertEquals(ExecutionStatus.SUCCESS, updated.getStatus());
        assertEquals(1, updated.getAttemptsCount());
        verify(paymentClient, times(1)).submitTransfer(any(), anyString());
        verify(paymentClient, times(1)).getTransferByReference(anyString());
    }

    @Test
    void testExecutionCutOff_PausedOrder_SkipsPayment() {
        StandingOrderResponse pausedSo = new StandingOrderResponse();
        pausedSo.setStatus(OrderStatus.PAUSED);
        when(standingOrderClient.getStandingOrder(anyString())).thenReturn(pausedSo);

        Execution exec = new Execution();
        exec.setStandingOrderId("so-9003");
        exec.setScheduledOccurrenceUtc("2026-10-25T01:00:00Z");
        exec.setStatus(ExecutionStatus.PENDING);
        exec = executionRepository.saveAndFlush(exec);

        schedulerService.processExecution(exec);

        Execution updated = executionRepository.findById(exec.getId()).orElseThrow();
        assertEquals(ExecutionStatus.CANCELLED, updated.getStatus());
        verify(paymentClient, never()).submitTransfer(any(), anyString());
    }

    @Test
    void testTransactionalOutbox_InsertsEventAtomically() {
        StandingOrderResponse so = new StandingOrderResponse();
        so.setStatus(OrderStatus.ACTIVE);
        when(standingOrderClient.getStandingOrder(anyString())).thenReturn(so);

        TransferResponse successRes = TransferResponse.completed(
                "TRF-mock", "key", "EWB-ASU-1001", "EWB-ASU-2001",
                BigDecimal.valueOf(5000), BigDecimal.valueOf(15000), BigDecimal.valueOf(6000),
                "LED-D-1", "LED-C-2", Instant.now().toString()
        );
        when(paymentClient.submitTransfer(any(), anyString())).thenReturn(successRes);

        Execution exec = new Execution();
        exec.setStandingOrderId("so-9004");
        exec.setScheduledOccurrenceUtc("2026-10-25T01:00:00Z");
        exec.setCustomerId("asuna");
        exec.setSourceAccountId("EWB-ASU-1001");
        exec.setDestinationAccountId("EWB-ASU-2001");
        exec.setAmount(BigDecimal.valueOf(5000));
        exec.setCurrency("PHP");
        exec.setStatus(ExecutionStatus.PENDING);
        exec = executionRepository.saveAndFlush(exec);

        schedulerService.processExecution(exec);

        List<OutboxEvent> events = outboxEventRepository.findByStatusOrderByCreatedAtAsc("PENDING");
        assertFalse(events.isEmpty());
        assertEquals("EXECUTION_COMPLETED", events.get(0).getEventType());
    }
}
