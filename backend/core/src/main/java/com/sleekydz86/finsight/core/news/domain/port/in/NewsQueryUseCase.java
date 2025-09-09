package com.sleekydz86.finsight.core.news.domain.port.in;

import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsDetailResponse;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsQueryRequest;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsSearchRequest;

public interface NewsQueryUseCase {
    Newses findAllByFilters(NewsQueryRequest request);

    PaginationResponse<Newses> searchNews(NewsSearchRequest request);

    NewsDetailResponse getNewsDetail(Long newsId);

    Newses getRelatedNews(Long newsId, int limit);

    Newses getPopularNews(int limit);

    Newses getLatestNews(int limit);

    Newses getNewsByCategory(String category, int limit);

    Newses getPersonalizedNews(String userEmail, int limit);
}