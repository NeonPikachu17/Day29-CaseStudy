package com.ewb.standingorder.controller;

import com.ewb.common.dto.DueStandingOrderDto;
import com.ewb.standingorder.service.StandingOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/standing-orders")
public class InternalStandingOrderController {

    private final StandingOrderService service;

    public InternalStandingOrderController(StandingOrderService service) {
        this.service = service;
    }

    @GetMapping("/due")
    public ResponseEntity<List<DueStandingOrderDto>> getDueOrders(
            @RequestParam(value = "cutoff", required = false) String cutoff) {
        return ResponseEntity.ok(service.getDueOrders(cutoff));
    }
}
