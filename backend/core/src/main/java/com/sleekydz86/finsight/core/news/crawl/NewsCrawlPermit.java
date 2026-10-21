package com.sleekydz86.finsight.core.news.crawl;

import com.sleekydz86.finsight.core.global.NewsProvider;

public record NewsCrawlPermit(NewsProvider provider, String owner, long fencingToken) {
}
