package com.ewb.standingorder.repository;

import com.ewb.common.model.OrderStatus;
import com.ewb.standingorder.entity.StandingOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StandingOrderRepository extends JpaRepository<StandingOrder, String> {
    List<StandingOrder> findByCustomerId(String customerId);
    List<StandingOrder> findByStatus(OrderStatus status);
}
