package com.sleekydz86.finsight.core.auth.service;

public class RateLimitSecurityInfo {
    private final boolean isBlocked;
    private final String blockReason;
    private final long blockExpiryTime;
    private final boolean isWhitelisted;
    private final int violationCount;
    private final long lastViolationTime;

    public RateLimitSecurityInfo(boolean isBlocked, String blockReason, long blockExpiryTime,
                                 boolean isWhitelisted, int violationCount, long lastViolationTime) {
        this.isBlocked = isBlocked;
        this.blockReason = blockReason;
        this.blockExpiryTime = blockExpiryTime;
        this.isWhitelisted = isWhitelisted;
        this.violationCount = violationCount;
        this.lastViolationTime = lastViolationTime;
    }

    public boolean isBlocked() { return isBlocked; }
    public String getBlockReason() { return blockReason; }
    public long getBlockExpiryTime() { return blockExpiryTime; }
    public boolean isWhitelisted() { return isWhitelisted; }
    public int getViolationCount() { return violationCount; }
    public long getLastViolationTime() { return lastViolationTime; }
}