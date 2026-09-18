package com.ewb.common.util;

public final class MaskingUtil {

    private MaskingUtil() {
        // Prevent instantiation
    }

    public static String maskAccount(String accountId) {
        if (accountId == null || accountId.length() <= 6) {
            return "****";
        }
        int len = accountId.length();
        String prefix = accountId.substring(0, 3);
        String suffix = accountId.substring(len - 4);
        return prefix + "-***-" + suffix;
    }
}
