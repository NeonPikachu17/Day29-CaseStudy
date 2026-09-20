package com.ewb.payment.controller;

import com.ewb.payment.dto.AccountResponse;
import com.ewb.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountController {

    private final PaymentService paymentService;

    public AccountController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<AccountResponse> get(@PathVariable String accountId) {
        return ResponseEntity.ok(paymentService.getAccount(accountId));
    }
}