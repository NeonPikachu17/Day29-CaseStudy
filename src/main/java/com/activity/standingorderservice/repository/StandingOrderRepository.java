package com.activity.standingorderservice.repository;

import com.activity.standingorderservice.entity.StandingOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StandingOrderRepository
        extends JpaRepository<StandingOrder, Long> {
}