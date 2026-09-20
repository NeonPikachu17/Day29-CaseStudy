package com.ewb.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.ewb.payment.entity.Transfer;
import com.ewb.payment.entity.TransferStatus;

public record TransferResponse(
        String reference,
        String idempotencyKey,
        String sourceAccountId,
        String destinationAccountId,
        BigDecimal amount,
        String currency,
        TransferStatus status,
        Long debitLedgerEntryId,
        Long creditLedgerEntryId,
        Instant createdAt
) {
    public static TransferResponse from(Transfer t) {
        return new TransferResponse(
                t.getReference(), t.getIdempotencyKey(),
                t.getSourceAccountId(), t.getDestinationAccountId(),
                t.getAmount(), t.getCurrency(), t.getStatus(),
                t.getDebitEntryId(), t.getCreditEntryId(), t.getCreatedAt());
    }
}