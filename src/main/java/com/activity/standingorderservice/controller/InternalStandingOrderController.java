package com.activity.standingorderservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalStandingOrderController {

    @GetMapping("/internal/standing-orders/due")
    public String dueOrders() {
        return "Due Standing Orders";
    }
}