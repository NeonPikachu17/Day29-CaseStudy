package com.ewb.payment.dto;

import java.math.BigDecimal;

import com.ewb.payment.entity.Account;
import com.ewb.payment.entity.AccountStatus;

public record AccountResponse(
        String accountId,
        String holderName,
        BigDecimal balance,
        String currency,
        AccountStatus status
) {
    public static AccountResponse from(Account a) {
        return new AccountResponse(a.getAccountId(), a.getHolderName(), a.getBalance(), a.getCurrency(), a.getStatus());
    }
}