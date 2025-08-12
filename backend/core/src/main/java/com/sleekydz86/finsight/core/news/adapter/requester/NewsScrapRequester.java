package com.sleekydz86.finsight.core.news.adapter.requester;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.News;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface NewsScrapRequester {

    NewsProvider supports();

    CompletableFuture<List<News>> scrap(LocalDateTime publishTimeAfter, int limit);
}