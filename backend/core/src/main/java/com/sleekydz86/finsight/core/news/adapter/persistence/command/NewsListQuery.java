package com.sleekydz86.finsight.core.news.adapter.persistence.command;

import com.querydsl.core.BooleanBuilder;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsQueryRequest;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsSearchRequest;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class NewsListQuery {

    private NewsListQuery() {
    }

    static BooleanBuilder filters(QNewsJpaEntity news, NewsQueryRequest request) {
        BooleanBuilder where = new BooleanBuilder();
        if (request == null) {
            return where;
        }
        applyPeriod(news, request, where);
        applyKeyword(news, request, where);
        if (request.getSentimentType() != null) {
            where.and(news.sentimentType.eq(request.getSentimentType()));
        }
        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            where.and(news.targetCategories.any().in(request.getCategories()));
        }
        if (request.getProviders() != null && !request.getProviders().isEmpty()) {
            where.and(news.newsProvider.in(request.getProviders()));
        }
        return where;
    }

    static NewsQueryRequest fromSearch(NewsSearchRequest request) {
        if (request == null) {
            return new NewsQueryRequest(null, null, null, null, null, null);
        }
        return new NewsQueryRequest(
                request.getStartDate(),
                request.getEndDate(),
                request.getSentimentType(),
                request.getKeyword(),
                request.getCategories(),
                request.getProviders());
    }

    static long offset(NewsSearchRequest request) {
        int page = request == null || request.getPage() < 0 ? 0 : request.getPage();
        return page * pageSize(request);
    }

    static long pageSize(NewsSearchRequest request) {
        if (request == null || request.getSize() <= 0) {
            return 20L;
        }
        return request.getSize();
    }

    static TargetCategory category(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return TargetCategory.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    static List<TargetCategory> categories(List<String> names) {
        List<TargetCategory> parsed = new ArrayList<>();
        if (names == null) {
            return parsed;
        }
        for (String name : names) {
            TargetCategory category = category(name);
            if (category != null) {
                parsed.add(category);
            }
        }
        return parsed;
    }

    private static void applyPeriod(QNewsJpaEntity news, NewsQueryRequest request, BooleanBuilder where) {
        if (request.getStartDate() != null) {
            where.and(news.newsPublishedTime.goe(request.getStartDate()));
        }
        if (request.getEndDate() != null) {
            where.and(news.newsPublishedTime.loe(request.getEndDate()));
        }
    }

    private static void applyKeyword(QNewsJpaEntity news, NewsQueryRequest request, BooleanBuilder where) {
        if (request.getKeyword() == null || request.getKeyword().isBlank()) {
            return;
        }
        String keyword = request.getKeyword().trim();
        where.and(news.originalTitle.contains(keyword)
                .or(news.originalContent.contains(keyword))
                .or(news.translatedTitle.contains(keyword))
                .or(news.translatedContent.contains(keyword)));
    }
}
