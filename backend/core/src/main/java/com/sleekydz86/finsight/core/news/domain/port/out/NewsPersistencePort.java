package com.sleekydz86.finsight.core.news.domain.port.out;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsQueryRequest;

import java.util.List;
import java.util.Optional;

public interface NewsPersistencePort {

    Newses saveAllNews(List<News> newses);

    Newses findByOverviewIsNull();

    Newses findAllByFilters(NewsQueryRequest request);

    Optional<News> findById(Long newsId);
}