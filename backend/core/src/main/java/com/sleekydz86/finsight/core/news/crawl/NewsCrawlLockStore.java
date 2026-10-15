package com.sleekydz86.finsight.core.news.crawl;

import com.sleekydz86.finsight.core.global.NewsProvider;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NewsCrawlLockStore {

    Optional<Long> acquire(NewsProvider provider, String owner, LocalDateTime expiresAt, LocalDateTime now);

    boolean release(NewsProvider provider, String owner, long fencingToken);

    boolean held(NewsProvider provider, String owner, long fencingToken, LocalDateTime now);
}
