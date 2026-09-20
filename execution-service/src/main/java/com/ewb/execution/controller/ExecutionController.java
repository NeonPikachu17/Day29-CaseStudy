package com.ewb.execution.controller;

import com.ewb.common.model.ExecutionStatus;
import com.ewb.execution.dto.ExecutionHistoryDto;
import com.ewb.execution.entity.Execution;
import com.ewb.execution.repository.ExecutionRepository;
import com.ewb.execution.service.SchedulerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class ExecutionController {

    private final ExecutionRepository executionRepository;
    private final SchedulerService schedulerService;

    public ExecutionController(ExecutionRepository executionRepository,
                               SchedulerService schedulerService) {
        this.executionRepository = executionRepository;
        this.schedulerService = schedulerService;
    }

    @GetMapping("/standing-orders/{id}/executions")
    public ResponseEntity<List<ExecutionHistoryDto>> getExecutionHistory(
            @PathVariable("id") String standingOrderId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {

        List<Execution> executions = executionRepository.findByStandingOrderIdOrderByCreatedAtDesc(standingOrderId);

        // Filter for customer persona if role is customer
        if ("ROLE_CUSTOMER".equals(userRole) && userId != null) {
            executions = executions.stream()
                    .filter(e -> userId.equals(e.getCustomerId()))
                    .collect(Collectors.toList());
        }

        List<ExecutionHistoryDto> dtoList = executions.stream().map(e -> new ExecutionHistoryDto(
                e.getId(),
                e.getStandingOrderId(),
                e.getScheduledOccurrenceUtc(),
                e.getInstructionVersion(),
                e.getStatus(),
                e.getPaymentReference(),
                e.getAttemptsCount(),
                e.getCreatedAt() != null ? e.getCreatedAt().toString() : null,
                e.getCompletedAt() != null ? e.getCompletedAt().toString() : null
        )).collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }

    @PostMapping("/internal/executions/{id}/recover")
    public ResponseEntity<Map<String, Object>> recoverExecution(
            @PathVariable("id") String executionId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {

        if (userRole != null && !"ROLE_OPERATIONS".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Execution execution = executionRepository.findById(executionId).orElse(null);
        if (execution == null) {
            return ResponseEntity.notFound().build();
        }

        // Retry processing execution
        schedulerService.processExecution(execution);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "RECOVERED");
        response.put("executionId", execution.getId());
        response.put("currentStatus", execution.getStatus());
        response.put("paymentReference", execution.getPaymentReference());

        return ResponseEntity.ok(response);
    }
}
