package com.sleekydz86.finsight.core.user.service;

import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.out.dto.UserResponse;

public interface UserCacheService {
    UserResponse getUserFromCache(Long userId);
    void cacheUser(User user);
    void cacheUserResponse(UserResponse userResponse);
    UserResponse getUserProfileFromCache(Long userId);
    void cacheUserProfile(UserResponse userResponse);
    void evictUserCache(Long userId);
    void evictAllUserCache();
    UserCacheStatistics getCacheStatistics();

    class UserCacheStatistics {
        private final long cacheHits;
        private final long cacheMisses;
        private final long cacheErrors;
        private final double hitRate;
        private final long totalRequests;
        private final boolean cacheAvailable;

        public UserCacheStatistics(long cacheHits, long cacheMisses, long cacheErrors,
                                   double hitRate, long totalRequests, boolean cacheAvailable) {
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
            this.cacheErrors = cacheErrors;
            this.hitRate = hitRate;
            this.totalRequests = totalRequests;
            this.cacheAvailable = cacheAvailable;
        }

        public static UserCacheStatistics empty() {
            return new UserCacheStatistics(0, 0, 0, 0.0, 0, false);
        }

        public long getCacheHits() { return cacheHits; }
        public long getCacheMisses() { return cacheMisses; }
        public long getCacheErrors() { return cacheErrors; }
        public double getHitRate() { return hitRate; }
        public long getTotalRequests() { return totalRequests; }
        public boolean isCacheAvailable() { return cacheAvailable; }

        @Override
        public String toString() {
            return "UserCacheStatistics{" +
                    "cacheHits=" + cacheHits +
                    ", cacheMisses=" + cacheMisses +
                    ", cacheErrors=" + cacheErrors +
                    ", hitRate=" + hitRate +
                    ", totalRequests=" + totalRequests +
                    ", cacheAvailable=" + cacheAvailable +
                    '}';
        }
    }
}