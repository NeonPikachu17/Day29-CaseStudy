package com.ewb.common.dto;

import com.ewb.common.model.TransferStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransferResponse {

    private String reference;
    private String idempotencyKey;
    private TransferStatus status;
    private String sourceAccountId;
    private String destinationAccountId;
    private BigDecimal amount;
    private BigDecimal sourceBalanceAfter;
    private BigDecimal destinationBalanceAfter;
    private String debitLedgerId;
    private String creditLedgerId;
    private String timestamp;
    private String errorCode;
    private String message;

    public TransferResponse() {
    }

    public static TransferResponse completed(String reference, String idempotencyKey, String sourceAccountId,
                                           String destinationAccountId, BigDecimal amount, BigDecimal sourceBalanceAfter,
                                           BigDecimal destinationBalanceAfter, String debitLedgerId, String creditLedgerId,
                                           String timestamp) {
        TransferResponse res = new TransferResponse();
        res.reference = reference;
        res.idempotencyKey = idempotencyKey;
        res.status = TransferStatus.COMPLETED;
        res.sourceAccountId = sourceAccountId;
        res.destinationAccountId = destinationAccountId;
        res.amount = amount;
        res.sourceBalanceAfter = sourceBalanceAfter;
        res.destinationBalanceAfter = destinationBalanceAfter;
        res.debitLedgerId = debitLedgerId;
        res.creditLedgerId = creditLedgerId;
        res.timestamp = timestamp;
        return res;
    }

    public static TransferResponse failed(String reference, String idempotencyKey, String errorCode, String message) {
        TransferResponse res = new TransferResponse();
        res.reference = reference;
        res.idempotencyKey = idempotencyKey;
        res.status = TransferStatus.FAILED;
        res.errorCode = errorCode;
        res.message = message;
        return res;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public void setStatus(TransferStatus status) {
        this.status = status;
    }

    public String getSourceAccountId() {
        return sourceAccountId;
    }

    public void setSourceAccountId(String sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public String getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(String destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getSourceBalanceAfter() {
        return sourceBalanceAfter;
    }

    public void setSourceBalanceAfter(BigDecimal sourceBalanceAfter) {
        this.sourceBalanceAfter = sourceBalanceAfter;
    }

    public BigDecimal getDestinationBalanceAfter() {
        return destinationBalanceAfter;
    }

    public void setDestinationBalanceAfter(BigDecimal destinationBalanceAfter) {
        this.destinationBalanceAfter = destinationBalanceAfter;
    }

    public String getDebitLedgerId() {
        return debitLedgerId;
    }

    public void setDebitLedgerId(String debitLedgerId) {
        this.debitLedgerId = debitLedgerId;
    }

    public String getCreditLedgerId() {
        return creditLedgerId;
    }

    public void setCreditLedgerId(String creditLedgerId) {
        this.creditLedgerId = creditLedgerId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
