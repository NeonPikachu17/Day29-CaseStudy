package com.ewb.payment.service;

import com.ewb.payment.dto.TransferRequest;
import com.ewb.payment.dto.TransferResponse;
import com.ewb.payment.entity.*;
import com.ewb.payment.exception.PaymentException;
import com.ewb.payment.repository.AccountRepository;
import com.ewb.payment.repository.IdempotencyRecordRepository;
import com.ewb.payment.repository.LedgerEntryRepository;
import com.ewb.payment.repository.TransferRepository;
import com.ewb.payment.util.AccountMasker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Runs the whole transfer inside ONE database transaction:
 * balances + 2 ledger entries + transfer record + idempotency record commit together or not at all.
 * Separate bean from PaymentService so the simulated timeout can be thrown AFTER the commit.
 */
@Service
public class TransferProcessor {

    private static final Logger log = LoggerFactory.getLogger(TransferProcessor.class);

    private final AccountRepository accounts;
    private final LedgerEntryRepository ledger;
    private final TransferRepository transfers;
    private final IdempotencyRecordRepository idempotency;
    private final ObjectMapper objectMapper;

    public TransferProcessor(AccountRepository accounts,
                             LedgerEntryRepository ledger,
                             TransferRepository transfers,
                             IdempotencyRecordRepository idempotency,
                             ObjectMapper objectMapper) {
        this.accounts = accounts;
        this.ledger = ledger;
        this.transfers = transfers;
        this.idempotency = idempotency;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TransferResponse execute(String idempotencyKey, TransferRequest request) {
        BigDecimal amount = request.amount().stripTrailingZeros().setScale(2, RoundingMode.UNNECESSARY);
        String currency = request.currency().toUpperCase(Locale.ROOT);
        String sourceId = request.sourceAccountId();
        String destId = request.destinationAccountId();
        String hash = fingerprint(sourceId, destId, amount, currency);

        // 1. Idempotency: same key + same payload -> return original result, move nothing.
        Optional<TransferResponse> replay = replayIfKnown(idempotencyKey, hash);
        if (replay.isPresent()) {
            return replay.get();
        }

        // 2. Lock both accounts (always in sorted order -> no deadlocks).
        Map<String, Account> locked = lockAccounts(sourceId, destId);

        // 3. Re-check idempotency now that we hold the locks (a concurrent duplicate may have just committed).
        replay = replayIfKnown(idempotencyKey, hash);
        if (replay.isPresent()) {
            return replay.get();
        }

        Account source = locked.get(sourceId);
        Account destination = locked.get(destId);

        // 4. Business rules
        if (source.getStatus() == AccountStatus.FROZEN) {
            throw new PaymentException(HttpStatus.UNPROCESSABLE_ENTITY, "ACCOUNT_FROZEN",
                    "Source account " + AccountMasker.mask(sourceId) + " is frozen");
        }
        if (!source.getCurrency().equals(currency) || !destination.getCurrency().equals(currency)) {
            throw new PaymentException(HttpStatus.UNPROCESSABLE_ENTITY, "CURRENCY_MISMATCH",
                    "Transfer currency does not match the account currency");
        }
        if (source.getBalance().compareTo(amount) < 0) {
            throw new PaymentException(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_FUNDS",
                    "Source account " + AccountMasker.mask(sourceId) + " has insufficient funds");
        }

        // 5. Atomic double-entry posting
        String reference = (request.reference() == null || request.reference().isBlank())
                ? idempotencyKey
                : request.reference();
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        source.setBalance(source.getBalance().subtract(amount));
        destination.setBalance(destination.getBalance().add(amount));
        accounts.save(source);
        accounts.save(destination);

        LedgerEntry debit = ledger.save(new LedgerEntry(reference, sourceId, LedgerEntryType.DEBIT, amount, currency, now));
        LedgerEntry credit = ledger.save(new LedgerEntry(reference, destId, LedgerEntryType.CREDIT, amount, currency, now));

        Transfer transfer = transfers.save(new Transfer(reference, idempotencyKey, hash, sourceId, destId,
                amount, currency, TransferStatus.COMPLETED, debit.getId(), credit.getId(), now));

        TransferResponse response = TransferResponse.from(transfer);
        idempotency.save(new IdempotencyRecord(idempotencyKey, hash, reference, toJson(response), now));

        log.info("Transfer {} COMPLETED: {} -> {} amount {} {}", reference,
                AccountMasker.mask(sourceId), AccountMasker.mask(destId), amount, currency);
        return response;
    }

    private Optional<TransferResponse> replayIfKnown(String key, String hash) {
        return idempotency.findByIdempotencyKey(key).map(record -> {
            if (!record.getRequestHash().equals(hash)) {
                throw new PaymentException(HttpStatus.CONFLICT, "IDEMPOTENCY_MISMATCH",
                        "Idempotency key '" + key + "' was already used with different payment details");
            }
            log.info("Idempotent replay for key {} - returning original result, no funds moved", key);
            return fromJson(record.getResponseJson());
        });
    }

    private Map<String, Account> lockAccounts(String sourceId, String destId) {
        Map<String, Account> locked = new HashMap<>();
        for (String id : new TreeSet<>(List.of(sourceId, destId))) {
            Account account = accounts.findByIdForUpdate(id).orElseThrow(() ->
                    new PaymentException(HttpStatus.UNPROCESSABLE_ENTITY, "ACCOUNT_NOT_FOUND",
                            "Account not found: " + AccountMasker.mask(id)));
            locked.put(id, account);
        }
        return locked;
    }

    private static String fingerprint(String source, String dest, BigDecimal amount, String currency) {
        String raw = String.join("|", source, dest, amount.toPlainString(), currency);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String toJson(TransferResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise transfer response", e);
        }
    }

    private TransferResponse fromJson(String json) {
        try {
            return objectMapper.readValue(json, TransferResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not read stored idempotent response", e);
        }
    }
}