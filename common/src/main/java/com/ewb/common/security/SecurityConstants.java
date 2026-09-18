package com.ewb.common.security;

public final class SecurityConstants {

    private SecurityConstants() {
        // Prevent instantiation
    }

    // Downstream forwarded HTTP headers injected by Gateway
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";
    public static final String HEADER_USER_ACCOUNTS = "X-User-Accounts";

    // System roles
    public static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";
    public static final String ROLE_OPERATIONS = "ROLE_OPERATIONS";
    public static final String ROLE_AUDITOR = "ROLE_AUDITOR";

    // JWT Configuration
    public static final String JWT_SECRET = "EastWestBankStandingOrderPlatformSecretKey2026SAOEnterpriseLTS!";
    public static final String JWT_ISSUER = "ewb-gateway-idp";
    public static final long JWT_EXPIRATION_MS = 86_400_000L; // 24 hours
}
