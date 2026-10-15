package com.sleekydz86.finsight.core.news.crawl;

import com.sleekydz86.finsight.core.global.NewsProvider;

import java.time.Duration;
import java.util.Optional;

public interface NewsCrawlRunGate {

    Optional<NewsCrawlPermit> tryAcquire(NewsProvider provider, String owner, Duration ttl);

    void release(NewsCrawlPermit permit);

    boolean stillHeld(NewsCrawlPermit permit);
}
