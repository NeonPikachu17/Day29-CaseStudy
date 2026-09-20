package com.ewb.standingorder.config;

import com.ewb.common.model.Frequency;
import com.ewb.common.model.OrderStatus;
import com.ewb.standingorder.entity.AuditLog;
import com.ewb.standingorder.entity.StandingOrder;
import com.ewb.standingorder.entity.StandingOrderVersion;
import com.ewb.standingorder.repository.AuditLogRepository;
import com.ewb.standingorder.repository.StandingOrderRepository;
import com.ewb.standingorder.repository.StandingOrderVersionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    private final StandingOrderRepository repository;
    private final StandingOrderVersionRepository versionRepository;
    private final AuditLogRepository auditLogRepository;

    public DataSeeder(StandingOrderRepository repository,
                      StandingOrderVersionRepository versionRepository,
                      AuditLogRepository auditLogRepository) {
        this.repository = repository;
        this.versionRepository = versionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void run(String... args) {
        if (!repository.existsById("so-9001")) {
            StandingOrder so = new StandingOrder(
                    "so-9001",
                    "asuna",
                    "EWB-ASU-1001",
                    "EWB-ASU-2001",
                    new BigDecimal("5000.00"),
                    "PHP",
                    Frequency.MONTHLY,
                    25,
                    "09:00",
                    "Asia/Manila",
                    "2026-10-25",
                    "2026-10-25T09:00:00+08:00",
                    OrderStatus.ACTIVE,
                    1,
                    "2026-09-18T08:00:00Z"
            );
            repository.save(so);

            StandingOrderVersion v = new StandingOrderVersion("so-9001", 1, new BigDecimal("5000.00"), 25, LocalDateTime.now());
            versionRepository.save(v);

            AuditLog log = new AuditLog("so-9001", "SYSTEM", "ORDER", null, "SEEDED", LocalDateTime.now());
            auditLogRepository.save(log);
        }
    }
}
