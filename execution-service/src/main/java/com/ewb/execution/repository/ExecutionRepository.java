package com.ewb.execution.repository;

import com.ewb.common.model.ExecutionStatus;
import com.ewb.execution.entity.Execution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExecutionRepository extends JpaRepository<Execution, String> {

    List<Execution> findByStandingOrderIdOrderByCreatedAtDesc(String standingOrderId);

    Optional<Execution> findByStandingOrderIdAndScheduledOccurrenceUtc(String standingOrderId, String scheduledOccurrenceUtc);

    List<Execution> findByStatus(ExecutionStatus status);

    @org.springframework.transaction.annotation.Transactional
    @Modifying
    @Query("UPDATE Execution e SET e.status = :claimedStatus, e.workerId = :workerId, e.leaseExpiresAt = :leaseExpiresAt " +
           "WHERE e.id = :id AND (e.status = :pendingStatus OR (e.status = :claimedStatus AND e.leaseExpiresAt < :now))")
    int claimExecutionLease(@Param("id") String id,
                            @Param("workerId") String workerId,
                            @Param("leaseExpiresAt") Instant leaseExpiresAt,
                            @Param("now") Instant now,
                            @Param("pendingStatus") ExecutionStatus pendingStatus,
                            @Param("claimedStatus") ExecutionStatus claimedStatus);
}
