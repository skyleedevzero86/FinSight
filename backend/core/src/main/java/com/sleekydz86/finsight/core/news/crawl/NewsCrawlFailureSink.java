package com.sleekydz86.finsight.core.news.crawl;

import com.sleekydz86.finsight.core.global.NewsProvider;

public interface NewsCrawlFailureSink {

    void record(NewsProvider provider, Throwable error);
}
