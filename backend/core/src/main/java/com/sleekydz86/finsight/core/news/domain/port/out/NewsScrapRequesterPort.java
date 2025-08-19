package com.sleekydz86.finsight.core.news.domain.port.out;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.News;

import java.util.List;

public interface NewsScrapRequesterPort {

    List<News> scrap(NewsProvider newsProvider);
}