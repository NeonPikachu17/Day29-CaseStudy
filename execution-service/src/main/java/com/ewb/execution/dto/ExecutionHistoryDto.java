package com.ewb.execution.dto;

import com.ewb.common.model.ExecutionStatus;

public class ExecutionHistoryDto {

    private String executionId;
    private String standingOrderId;
    private String scheduledOccurrenceUtc;
    private Integer instructionVersion;
    private ExecutionStatus status;
    private String paymentReference;
    private int attemptsCount;
    private String createdAt;
    private String completedAt;

    public ExecutionHistoryDto() {}

    public ExecutionHistoryDto(String executionId, String standingOrderId, String scheduledOccurrenceUtc,
                               Integer instructionVersion, ExecutionStatus status, String paymentReference,
                               int attemptsCount, String createdAt, String completedAt) {
        this.executionId = executionId;
        this.standingOrderId = standingOrderId;
        this.scheduledOccurrenceUtc = scheduledOccurrenceUtc;
        this.instructionVersion = instructionVersion;
        this.status = status;
        this.paymentReference = paymentReference;
        this.attemptsCount = attemptsCount;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }

    public String getStandingOrderId() { return standingOrderId; }
    public void setStandingOrderId(String standingOrderId) { this.standingOrderId = standingOrderId; }

    public String getScheduledOccurrenceUtc() { return scheduledOccurrenceUtc; }
    public void setScheduledOccurrenceUtc(String scheduledOccurrenceUtc) { this.scheduledOccurrenceUtc = scheduledOccurrenceUtc; }

    public Integer getInstructionVersion() { return instructionVersion; }
    public void setInstructionVersion(Integer instructionVersion) { this.instructionVersion = instructionVersion; }

    public ExecutionStatus getStatus() { return status; }
    public void setStatus(ExecutionStatus status) { this.status = status; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public int getAttemptsCount() { return attemptsCount; }
    public void setAttemptsCount(int attemptsCount) { this.attemptsCount = attemptsCount; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
}
