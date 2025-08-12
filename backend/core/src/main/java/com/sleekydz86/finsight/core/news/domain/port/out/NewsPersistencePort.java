package com.sleekydz86.finsight.core.news.domain.port.out;

import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsQueryRequest;

public interface NewsPersistencePort {

    Newses saveAllNews(Newses newses);

    Newses findAllByFilters(NewsQueryRequest request);
}