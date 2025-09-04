package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsCommandUseCase;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsQueryUseCase;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsQueryRequest;
import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/news")
public class NewsController {

    private final NewsCommandUseCase newsCommandUseCase;
    private final NewsQueryUseCase newsQueryUseCase;

    public NewsController(NewsCommandUseCase newsCommandUseCase, NewsQueryUseCase newsQueryUseCase) {
        this.newsCommandUseCase = newsCommandUseCase;
        this.newsQueryUseCase = newsQueryUseCase;
    }

    @PostMapping("/scrap")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Newses> scrapNews() {
        Newses newses = newsCommandUseCase.scrapNewses().join();
        return ResponseEntity.ok(newses);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Newses> searchNews(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) SentimentType sentimentType,
            @RequestParam(required = false) List<NewsProvider> newsProviders,
            @RequestParam(required = false) List<TargetCategory> categories) {

        NewsQueryRequest request = new NewsQueryRequest(startDate, endDate, sentimentType, newsProviders, categories);
        Newses newses = newsQueryUseCase.findAllByFilters(request);
        return ResponseEntity.ok(newses);
    }

    @GetMapping("/personalized/{userId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Newses>> getPersonalizedNews(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        NewsQueryRequest request = new NewsQueryRequest(null, null, null, null, null);
        Newses newses = newsQueryUseCase.findAllByFilters(request);
        return ResponseEntity.ok(ApiResponse.success(newses, "개인화된 뉴스 조회 성공"));
    }

    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<List<News>>> getTrendingNews() {
        NewsQueryRequest request = new NewsQueryRequest(null, null, null, null, null);
        Newses newses = newsQueryUseCase.findAllByFilters(request);
        List<News> trendingNews = newses.getNewses().stream()
                .limit(10)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(trendingNews, "트렌딩 뉴스 조회 성공"));
    }
}