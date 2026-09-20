package com.ewb.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.ewb.payment.dto.TransferRequest;
import com.ewb.payment.dto.TransferResponse;
import com.ewb.payment.service.PaymentService;

import jakarta.validation.Valid;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/transfers")
    public ResponseEntity<TransferResponse> transfer(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Simulate-Timeout", defaultValue = "false") boolean simulateTimeout,
            @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(paymentService.transfer(idempotencyKey, request, simulateTimeout));
    }

    @GetMapping("/transfers/by-reference/{reference}")
    public ResponseEntity<TransferResponse> byReference(@PathVariable String reference) {
        return ResponseEntity.ok(paymentService.findByReference(reference));
    }
}