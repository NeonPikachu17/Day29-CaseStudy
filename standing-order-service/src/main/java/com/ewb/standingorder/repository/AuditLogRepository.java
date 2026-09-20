package com.ewb.standingorder.repository;

import com.ewb.standingorder.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByStandingOrderIdOrderByTimestampDesc(String standingOrderId);
}
