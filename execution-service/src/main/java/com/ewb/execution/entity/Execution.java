package com.ewb.execution.entity;

import com.ewb.common.model.ExecutionStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
    name = "executions",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"standing_order_id", "scheduled_occurrence_utc"}
    )
)
public class Execution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "standing_order_id", nullable = false)
    private String standingOrderId;

    @Column(name = "scheduled_occurrence_utc", nullable = false)
    private String scheduledOccurrenceUtc;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "source_account_id")
    private String sourceAccountId;

    @Column(name = "destination_account_id")
    private String destinationAccountId;

    @Column(name = "amount")
    private java.math.BigDecimal amount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "instruction_version")
    private Integer instructionVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;

    @Column(name = "payment_reference")
    private String paymentReference;

    @Column(name = "worker_id")
    private String workerId;

    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    @Column(name = "attempts_count")
    private int attemptsCount = 0;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = ExecutionStatus.PENDING;
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStandingOrderId() { return standingOrderId; }
    public void setStandingOrderId(String standingOrderId) { this.standingOrderId = standingOrderId; }

    public String getScheduledOccurrenceUtc() { return scheduledOccurrenceUtc; }
    public void setScheduledOccurrenceUtc(String scheduledOccurrenceUtc) { this.scheduledOccurrenceUtc = scheduledOccurrenceUtc; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getSourceAccountId() { return sourceAccountId; }
    public void setSourceAccountId(String sourceAccountId) { this.sourceAccountId = sourceAccountId; }

    public String getDestinationAccountId() { return destinationAccountId; }
    public void setDestinationAccountId(String destinationAccountId) { this.destinationAccountId = destinationAccountId; }

    public java.math.BigDecimal getAmount() { return amount; }
    public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Integer getInstructionVersion() { return instructionVersion; }
    public void setInstructionVersion(Integer instructionVersion) { this.instructionVersion = instructionVersion; }

    public ExecutionStatus getStatus() { return status; }
    public void setStatus(ExecutionStatus status) { this.status = status; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }

    public Instant getLeaseExpiresAt() { return leaseExpiresAt; }
    public void setLeaseExpiresAt(Instant leaseExpiresAt) { this.leaseExpiresAt = leaseExpiresAt; }

    public int getAttemptsCount() { return attemptsCount; }
    public void setAttemptsCount(int attemptsCount) { this.attemptsCount = attemptsCount; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
