package com.sleekydz86.finsight.core.news.domain.port.in;

import com.sleekydz86.finsight.core.news.domain.Newses;

import java.util.concurrent.CompletableFuture;

public interface NewsCommandUseCase {

    
    CompletableFuture<Newses> scrapNewses();
}