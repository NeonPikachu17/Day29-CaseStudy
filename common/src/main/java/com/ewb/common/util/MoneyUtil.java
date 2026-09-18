package com.ewb.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtil {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private MoneyUtil() {
        // Prevent instantiation
    }

    public static BigDecimal format(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);
        }
        return amount.setScale(SCALE, ROUNDING_MODE);
    }

    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isGreaterThanOrEqual(BigDecimal first, BigDecimal second) {
        if (first == null || second == null) {
            return false;
        }
        return format(first).compareTo(format(second)) >= 0;
    }

    public static BigDecimal add(BigDecimal first, BigDecimal second) {
        BigDecimal a = first != null ? first : BigDecimal.ZERO;
        BigDecimal b = second != null ? second : BigDecimal.ZERO;
        return a.add(b).setScale(SCALE, ROUNDING_MODE);
    }

    public static BigDecimal subtract(BigDecimal first, BigDecimal second) {
        BigDecimal a = first != null ? first : BigDecimal.ZERO;
        BigDecimal b = second != null ? second : BigDecimal.ZERO;
        return a.subtract(b).setScale(SCALE, ROUNDING_MODE);
    }
}
