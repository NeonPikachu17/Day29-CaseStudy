package com.ewb.payment.config;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ewb.payment.entity.Account;
import com.ewb.payment.entity.AccountStatus;
import com.ewb.payment.repository.AccountRepository;
import com.ewb.payment.repository.IdempotencyRecordRepository;
import com.ewb.payment.repository.LedgerEntryRepository;
import com.ewb.payment.repository.TransferRepository;

@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AccountRepository accounts;
    private final LedgerEntryRepository ledger;
    private final TransferRepository transfers;
    private final IdempotencyRecordRepository idempotency;

    public DataSeeder(AccountRepository accounts, LedgerEntryRepository ledger,
                      TransferRepository transfers, IdempotencyRecordRepository idempotency) {
        this.accounts = accounts;
        this.ledger = ledger;
        this.transfers = transfers;
        this.idempotency = idempotency;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (accounts.count() == 0) {
            insertSeedAccounts();
        }
    }

    /** Wipes everything and restores the original balances. Used by the tests. */
    @Transactional
    public void reset() {
        idempotency.deleteAllInBatch();
        transfers.deleteAllInBatch();
        ledger.deleteAllInBatch();
        accounts.deleteAllInBatch();
        insertSeedAccounts();
    }

    private void insertSeedAccounts() {
        accounts.saveAll(List.of(
                new Account("EWB-ASU-1001", "Asuna Yuuki", new BigDecimal("20000.00"), "PHP", AccountStatus.ACTIVE),
                new Account("EWB-ASU-2001", "Asuna Yuuki", new BigDecimal("1000.00"), "PHP", AccountStatus.ACTIVE),
                new Account("EWB-KIR-5001", "Kazuto Kirigaya", new BigDecimal("50000.00"), "PHP", AccountStatus.ACTIVE),
                new Account("EWB-KLN-4001", "Ryoutarou Tsuboi", new BigDecimal("2000.00"), "PHP", AccountStatus.ACTIVE),
                new Account("EWB-HTH-3001", "Akihiko Kayaba", new BigDecimal("10000.00"), "PHP", AccountStatus.FROZEN)
        ));
        log.info("Seeded 5 SAO mock accounts into payment_db");
    }
}