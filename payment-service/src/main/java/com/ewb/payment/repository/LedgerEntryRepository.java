package com.ewb.payment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ewb.payment.entity.LedgerEntry;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByReference(String reference);
}