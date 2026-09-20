package com.ewb.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ewb.payment.dto.AccountResponse;
import com.ewb.payment.dto.TransferRequest;
import com.ewb.payment.dto.TransferResponse;
import com.ewb.payment.exception.PaymentException;
import com.ewb.payment.repository.AccountRepository;
import com.ewb.payment.repository.TransferRepository;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final TransferProcessor processor;
    private final TransferRepository transfers;
    private final AccountRepository accounts;

    public PaymentService(TransferProcessor processor, TransferRepository transfers, AccountRepository accounts) {
        this.processor = processor;
        this.transfers = transfers;
        this.accounts = accounts;
    }

    /** Not transactional itself: money commits inside TransferProcessor, THEN we may simulate a lost response. */
    public TransferResponse transfer(String idempotencyKey, TransferRequest request, boolean simulateTimeout) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED",
                    "The Idempotency-Key header is required");
        }
        if (idempotencyKey.length() > 128) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_INVALID",
                    "The Idempotency-Key header must be at most 128 characters");
        }
        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "SAME_ACCOUNT",
                    "Source and destination accounts must differ");
        }

        TransferResponse response;
        try {
            response = processor.execute(idempotencyKey, request);
        } catch (DataIntegrityViolationException e) {
            log.warn("Unique-constraint race for idempotency key {} - retrying as replay", idempotencyKey);
            response = processor.execute(idempotencyKey, request);
        }

        if (simulateTimeout) {
            throw new PaymentException(HttpStatus.GATEWAY_TIMEOUT, "SIMULATED_TIMEOUT",
                    "Simulated gateway timeout - the outcome is unknown to the caller");
        }
        return response;
    }

    @Transactional(readOnly = true)
    public TransferResponse findByReference(String reference) {
        return transfers.findByReference(reference)
                .or(() -> transfers.findByIdempotencyKey(reference))
                .map(TransferResponse::from)
                .orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "TRANSFER_NOT_FOUND",
                        "No transfer found for reference " + reference));
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountId) {
        return accounts.findById(accountId)
                .map(AccountResponse::from)
                .orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND",
                        "Account not found"));
    }
}