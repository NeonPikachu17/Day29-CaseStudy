package com.ewb.standingorder.controller;

import com.ewb.common.dto.CreateStandingOrderRequest;
import com.ewb.common.dto.StandingOrderResponse;
import com.ewb.standingorder.service.StandingOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/standing-orders")
public class StandingOrderController {

    private final StandingOrderService service;

    public StandingOrderController(StandingOrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<StandingOrderResponse> createStandingOrder(
            @RequestBody(required = false) CreateStandingOrderRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        StandingOrderResponse response = service.createStandingOrder(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<StandingOrderResponse>> getStandingOrders(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(service.getAllOrders(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StandingOrderResponse> getStandingOrderById(@PathVariable String id) {
        StandingOrderResponse response = service.getOrderById(id);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<StandingOrderResponse> pauseStandingOrder(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(service.pauseOrder(id, userId));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<StandingOrderResponse> resumeStandingOrder(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(service.resumeOrder(id, userId));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<StandingOrderResponse> cancelStandingOrder(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(service.cancelOrder(id, userId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<StandingOrderResponse> patchStandingOrder(
            @PathVariable String id,
            @RequestBody Map<String, Object> updates,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(service.patchOrder(id, updates, userId));
    }
}
