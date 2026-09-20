package com.ewb.payment.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "transfers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_transfer_reference", columnNames = "reference"),
        @UniqueConstraint(name = "uk_transfer_idempotency_key", columnNames = "idempotency_key")
})
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String reference;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "source_account_id", nullable = false, length = 32)
    private String sourceAccountId;

    @Column(name = "destination_account_id", nullable = false, length = 32)
    private String destinationAccountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransferStatus status;

    @Column(name = "debit_entry_id", nullable = false)
    private Long debitEntryId;

    @Column(name = "credit_entry_id", nullable = false)
    private Long creditEntryId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Transfer() {
    }

    public Transfer(String reference, String idempotencyKey, String requestHash,
                    String sourceAccountId, String destinationAccountId,
                    BigDecimal amount, String currency, TransferStatus status,
                    Long debitEntryId, Long creditEntryId, Instant createdAt) {
        this.reference = reference;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.debitEntryId = debitEntryId;
        this.creditEntryId = creditEntryId;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public String getSourceAccountId() { return sourceAccountId; }
    public String getDestinationAccountId() { return destinationAccountId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public TransferStatus getStatus() { return status; }
    public Long getDebitEntryId() { return debitEntryId; }
    public Long getCreditEntryId() { return creditEntryId; }
    public Instant getCreatedAt() { return createdAt; }
}