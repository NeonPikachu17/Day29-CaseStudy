package com.ewb.standingorder.repository;

import com.ewb.standingorder.entity.StandingOrderVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StandingOrderVersionRepository extends JpaRepository<StandingOrderVersion, Long> {
    List<StandingOrderVersion> findByStandingOrderIdOrderByVersionDesc(String standingOrderId);
}
