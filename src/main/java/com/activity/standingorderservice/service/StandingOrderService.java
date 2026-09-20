package com.activity.standingorderservice.service;

import com.activity.standingorderservice.entity.StandingOrder;
import com.activity.standingorderservice.repository.StandingOrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StandingOrderService {

    private final StandingOrderRepository repository;

    public StandingOrderService(StandingOrderRepository repository) {
        this.repository = repository;
    }

    public StandingOrder createStandingOrder() {

        StandingOrder order = StandingOrder.builder()
                .sourceAccountId("EWB-ASU-1001")
                .destinationAccountId("EWB-ASU-2001")
                .amount(5000.00)
                .status("ACTIVE")
                .build();

        return repository.save(order);
    }

    public List<StandingOrder> getAllOrders() {
        return repository.findAll();
    }

    public String pauseOrder(Long id) {

        StandingOrder order = repository.findById(id)
                .orElseThrow();

        order.setStatus("PAUSED");

        repository.save(order);

        return "Standing Order Paused";
    }

    public String resumeOrder(Long id) {

        StandingOrder order = repository.findById(id)
                .orElseThrow();

        order.setStatus("ACTIVE");

        repository.save(order);

        return "Standing Order Resumed";
    }

    public String cancelOrder(Long id) {

        StandingOrder order = repository.findById(id)
                .orElseThrow();

        order.setStatus("CANCELLED");

        repository.save(order);

        return "Standing Order Cancelled";
    }
}