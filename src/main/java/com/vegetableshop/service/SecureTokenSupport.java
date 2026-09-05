package com.vegetableshop.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

final class SecureTokenSupport {

    private static final SecureRandom RANDOM = new SecureRandom();

    private SecureTokenSupport() {
    }

    static String generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static boolean isValidFormat(String rawToken) {
        return rawToken != null && rawToken.matches("^[A-Za-z0-9_-]{43}$");
    }

    static String hash(String rawToken) {
        if (!isValidFormat(rawToken)) {
            throw new IllegalArgumentException("Token không đúng định dạng");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JVM không hỗ trợ SHA-256", exception);
        }
    }
}
