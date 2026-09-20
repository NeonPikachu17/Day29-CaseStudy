package com.ewb.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ewb.payment.entity.Transfer;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    Optional<Transfer> findByReference(String reference);

    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);
}