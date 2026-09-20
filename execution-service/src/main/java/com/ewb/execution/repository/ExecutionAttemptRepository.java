package com.ewb.execution.repository;

import com.ewb.execution.entity.ExecutionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionAttemptRepository extends JpaRepository<ExecutionAttempt, String> {
    List<ExecutionAttempt> findByExecutionIdOrderByAttemptNumberAsc(String executionId);
}
