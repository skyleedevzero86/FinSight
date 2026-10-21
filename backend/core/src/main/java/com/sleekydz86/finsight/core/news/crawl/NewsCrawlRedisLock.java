package com.sleekydz86.finsight.core.news.crawl;

import java.time.Duration;

public interface NewsCrawlRedisLock {

    boolean tryHold(String key, String owner, Duration ttl);

    void release(String key, String owner);
}
