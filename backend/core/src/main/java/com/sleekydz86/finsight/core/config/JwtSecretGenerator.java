package com.sleekydz86.finsight.core.config;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class JwtSecretGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String generateSecureSecret() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String generateVerySecureSecret() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static SecurityLevel evaluateSecurityLevel(String secret) {
        if (secret == null) return SecurityLevel.LOW;

        int bitLength = secret.length() * 6;

        if (bitLength >= 512) return SecurityLevel.HIGH;
        if (bitLength >= 256) return SecurityLevel.MEDIUM;
        return SecurityLevel.LOW;
    }

    public enum SecurityLevel {
        HIGH("높음 - 권장"),
        MEDIUM("보통 - 개선 권장"),
        LOW("낮음 - 즉시 개선 필요");

        private final String description;

        SecurityLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}