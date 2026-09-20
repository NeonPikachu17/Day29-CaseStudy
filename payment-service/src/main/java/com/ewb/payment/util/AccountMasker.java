package com.ewb.payment.util;

/** Masks account numbers for logs: EWB-ASU-1001 -> ********1001 */
public final class AccountMasker {

    private AccountMasker() {
    }

    public static String mask(String accountId) {
        if (accountId == null || accountId.length() <= 4) {
            return "****";
        }
        return "*".repeat(accountId.length() - 4) + accountId.substring(accountId.length() - 4);
    }
}