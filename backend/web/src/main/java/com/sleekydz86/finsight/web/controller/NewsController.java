package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsCommandUseCase;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsQueryUseCase;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsDetailResponse;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsQueryRequest;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsSearchRequest;
import com.sleekydz86.finsight.core.global.annotation.LogExecution;
import com.sleekydz86.finsight.core.global.annotation.PerformanceMonitor;
import com.sleekydz86.finsight.core.global.annotation.SecurityAudit;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.global.exception.SystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/news")
public class NewsController {

    private final NewsCommandUseCase newsCommandUseCase;
    private final NewsQueryUseCase newsQueryUseCase;

    @Autowired
    public NewsController(NewsCommandUseCase newsCommandUseCase, NewsQueryUseCase newsQueryUseCase) {
        this.newsCommandUseCase = newsCommandUseCase;
        this.newsQueryUseCase = newsQueryUseCase;
    }

    @PostMapping("/scrap")
    @LogExecution("뉴스 스크래핑 API")
    @PerformanceMonitor(threshold = 5000, metricName = "api.news.scrap")
    @SecurityAudit(action = "NEWS_SCRAP_API", resource = "NEWS_API", level = SecurityAudit.SecurityLevel.MEDIUM)
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<ApiResponse<News>> scrapNews(@RequestParam String url) {
        try {
            News news = newsCommandUseCase.scrapNews(url);
            return ResponseEntity.ok(ApiResponse.success(news));
        } catch (SystemException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("뉴스 스크래핑 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/{newsId}/analyze")
    @LogExecution("뉴스 AI 분석 API")
    @PerformanceMonitor(threshold = 10000, metricName = "api.news.analyze")
    @SecurityAudit(action = "NEWS_ANALYZE_API", resource = "NEWS_API", level = SecurityAudit.SecurityLevel.HIGH)
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<ApiResponse<News>> analyzeNews(@PathVariable Long newsId) {
        try {
            News news = newsCommandUseCase.analyzeNews(newsId);
            return ResponseEntity.ok(ApiResponse.success(news));
        } catch (SystemException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("뉴스 AI 분석 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/{newsId}/sentiment")
    @LogExecution("뉴스 감정 분석 API")
    @PerformanceMonitor(threshold = 3000, metricName = "api.news.sentiment")
    @SecurityAudit(action = "NEWS_SENTIMENT_API", resource = "NEWS_API", level = SecurityAudit.SecurityLevel.MEDIUM)
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<ApiResponse<News>> analyzeSentiment(@PathVariable Long newsId) {
        try {
            News news = newsCommandUseCase.analyzeSentiment(newsId);
            return ResponseEntity.ok(ApiResponse.success(news));
        } catch (SystemException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("뉴스 감정 분석 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/{newsId}")
    @LogExecution("뉴스 상세 조회 API")
    @PerformanceMonitor(threshold = 1000, metricName = "api.news.detail")
    @SecurityAudit(action = "NEWS_DETAIL_API", resource = "NEWS_API", level = SecurityAudit.SecurityLevel.LOW)
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<ApiResponse<NewsDetailResponse>> getNewsDetail(@PathVariable Long newsId) {
        try {
            NewsDetailResponse news = newsQueryUseCase.getNewsDetail(newsId);
            return ResponseEntity.ok(ApiResponse.success(news));
        } catch (SystemException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("뉴스 조회 중 오류가 발생했습니다."));
        }
    }

    @GetMapping
    @LogExecution("뉴스 목록 조회 API")
    @PerformanceMonitor(threshold = 2000, metricName = "api.news.list")
    @SecurityAudit(action = "NEWS_LIST_API", resource = "NEWS_API", level = SecurityAudit.SecurityLevel.LOW)
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<ApiResponse<PaginationResponse<Newses>>> getNewsList(
            @Valid NewsQueryRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Newses newses = newsQueryUseCase.getNewsList(request);

            PaginationResponse<Newses> paginationResponse = PaginationResponse.<Newses>builder()
                    .content(newses)
                    .page(page)
                    .size(size)
                    .totalElements(newses.getNewsList().size())
                    .totalPages((int) Math.ceil((double) newses.getNewsList().size() / size))
                    .first(page == 0)
                    .last(page >= (int) Math.ceil((double) newses.getNewsList().size() / size) - 1)
                    .build();

            return ResponseEntity.ok(ApiResponse.success(paginationResponse));
        } catch (SystemException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("뉴스 목록 조회 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/search")
    @LogExecution("뉴스 검색 API")
    @PerformanceMonitor(threshold = 3000, metricName = "api.news.search")
    @SecurityAudit(action = "NEWS_SEARCH_API", resource = "NEWS_API", level = SecurityAudit.SecurityLevel.LOW)
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<ApiResponse<PaginationResponse<Newses>>> searchNews(
            @Valid NewsSearchRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Newses newses = newsQueryUseCase.searchNews(request);

            PaginationResponse<Newses> paginationResponse = PaginationResponse.<Newses>builder()
                    .content(newses)
                    .page(page)
                    .size(size)
                    .totalElements(newses.getNewsList().size())
                    .totalPages((int) Math.ceil((double) newses.getNewsList().size() / size))
                    .first(page == 0)
                    .last(page >= (int) Math.ceil((double) newses.getNewsList().size() / size) - 1)
                    .build();

            return ResponseEntity.ok(ApiResponse.success(paginationResponse));
        } catch (SystemException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("뉴스 검색 중 오류가 발생했습니다."));
        }
    }
}