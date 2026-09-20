package com.activity.standingorderservice.repository;

import com.activity.standingorderservice.entity.StandingOrderVersion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StandingOrderVersionRepository
        extends JpaRepository<StandingOrderVersion, Long> {
}