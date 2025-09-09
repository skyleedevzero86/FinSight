package com.sleekydz86.finsight.core.auth.service;

public interface RateLimitServiceInterface {

    void checkRateLimit(String identifier, String action);
    int getCurrentCount(String identifier, String action);
    void resetLimit(String identifier, String action);
    boolean isRedisAvailable();

    void blockIdentifier(String identifier, String reason, long blockDurationMinutes);
    void unblockIdentifier(String identifier);
    boolean isIdentifierBlocked(String identifier);
    void addToWhitelist(String identifier);
    void removeFromWhitelist(String identifier);
    boolean isWhitelisted(String identifier);
    RateLimitSecurityInfo getSecurityInfo(String identifier);
}