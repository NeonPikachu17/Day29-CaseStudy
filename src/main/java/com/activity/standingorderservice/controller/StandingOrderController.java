package com.activity.standingorderservice.controller;

import com.activity.standingorderservice.entity.StandingOrder;
import com.activity.standingorderservice.service.StandingOrderService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class StandingOrderController {

    private final StandingOrderService service;

    public StandingOrderController(StandingOrderService service) {
        this.service = service;
    }

    @PostMapping("/standing-orders")
    public StandingOrder createStandingOrder() {
        return service.createStandingOrder();
    }

    @GetMapping("/standing-orders")
    public List<StandingOrder> getStandingOrders() {
        return service.getAllOrders();
    }

    @PostMapping("/standing-orders/{id}/pause")
    public String pauseStandingOrder(@PathVariable Long id) {
        return service.pauseOrder(id);
    }

    @PostMapping("/standing-orders/{id}/resume")
    public String resumeStandingOrder(@PathVariable Long id) {
        return service.resumeOrder(id);
    }

    @PostMapping("/standing-orders/{id}/cancel")
    public String cancelStandingOrder(@PathVariable Long id) {
        return service.cancelOrder(id);
    }
}

