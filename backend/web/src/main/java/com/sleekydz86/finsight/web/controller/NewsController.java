package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsCommandUseCase;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsQueryUseCase;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsDetailResponse;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsQueryRequest;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsSearchRequest;
import com.sleekydz86.finsight.core.global.annotation.CurrentUser;
import com.sleekydz86.finsight.core.global.annotation.LogExecution;
import com.sleekydz86.finsight.core.global.annotation.PerformanceMonitor;
import com.sleekydz86.finsight.core.global.annotation.SecurityAudit;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.global.exception.SystemException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/news")
public class NewsController {

    private final NewsCommandUseCase newsCommandUseCase;
    private final NewsQueryUseCase newsQueryUseCase;

    public NewsController(NewsCommandUseCase newsCommandUseCase, NewsQueryUseCase newsQueryUseCase) {
        this.newsCommandUseCase = newsCommandUseCase;
        this.newsQueryUseCase = newsQueryUseCase;
    }

    @PostMapping("/scrap")
    @LogExecution("뉴스 스크래핑 API")
    @PerformanceMonitor(threshold = 5000, metricName = "api.news.scrap")
    @SecurityAudit(action = "NEWS_SCRAP_API", resource = "NEWS_API", level = SecurityAudit.SecurityLevel.INFO)
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<ApiResponse<CompletableFuture<Newses>>> scrapNews(
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            CompletableFuture<Newses> newses = newsCommandUseCase.scrapNewses();
            return ResponseEntity.ok(ApiResponse.success(newses, "뉴스 스크래핑이 시작되었습니다"));
        } catch (SystemException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("뉴스 스크래핑 중 오류가 발생했습니다.", 500));
        }
    }

    @GetMapping("/{newsId}")
    @LogExecution("뉴스 상세 조회 API")
    @PerformanceMonitor(threshold = 1000, metricName = "api.news.detail")
    @SecurityAudit(action = "NEWS_DETAIL_API", resource = "NEWS_API", level = SecurityAudit.SecurityLevel.INFO)
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<ApiResponse<NewsDetailResponse>> getNewsDetail(
            @PathVariable Long newsId,
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            NewsDetailResponse news = newsQueryUseCase.getNewsDetail(newsId);
            return ResponseEntity.ok(ApiResponse.success(news, "뉴스 상세 조회에 성공했습니다"));
        } catch (SystemException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("뉴스 조회 중 오류가 발생했습니다.", 500));
        }
    }

    @GetMapping
    @LogExecution("뉴스 목록 조회 API")
    @PerformanceMonitor(threshold = 2000, metricName = "api.news.list")
    @SecurityAudit(action = "NEWS_LIST_API", resource = "NEWS_API", level = SecurityAudit.SecurityLevel.INFO)
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<ApiResponse<Newses>> getNewsList(
            @Valid NewsQueryRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            Newses newses = newsQueryUseCase.findAllByFilters(request);
            return ResponseEntity.ok(ApiResponse.success(newses, "뉴스 목록 조회에 성공했습니다"));
        } catch (SystemException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("뉴스 목록 조회 중 오류가 발생했습니다.", 500));
        }
    }

    @GetMapping("/search")
    @LogExecution("뉴스 검색 API")
    @PerformanceMonitor(threshold = 3000, metricName = "api.news.search")
    @SecurityAudit(action = "NEWS_SEARCH_API", resource = "NEWS_API", level = SecurityAudit.SecurityLevel.INFO)
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<ApiResponse<PaginationResponse<Newses>>> searchNews(
            @Valid NewsSearchRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            PaginationResponse<Newses> newses = newsQueryUseCase.searchNews(request);
            return ResponseEntity.ok(ApiResponse.success(newses, "뉴스 검색에 성공했습니다"));
        } catch (SystemException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("뉴스 검색 중 오류가 발생했습니다.", 500));
        }
    }

    @GetMapping("/popular")
    @LogExecution("인기 뉴스 조회 API")
    @PerformanceMonitor(threshold = 1000, metricName = "api.news.popular")
    @SecurityAudit(action = "NEWS_POPULAR_API", resource = "NEWS_API", level = SecurityAudit.SecurityLevel.INFO)
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<ApiResponse<Newses>> getPopularNews(
            @RequestParam(defaultValue = "10") int limit,
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            Newses newses = newsQueryUseCase.getPopularNews(limit);
            return ResponseEntity.ok(ApiResponse.success(newses, "인기 뉴스 조회에 성공했습니다"));
        } catch (SystemException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("인기 뉴스 조회 중 오류가 발생했습니다.", 500));
        }
    }

    @GetMapping("/latest")
    @LogExecution("최신 뉴스 조회 API")
    @PerformanceMonitor(threshold = 1000, metricName = "api.news.latest")
    @SecurityAudit(action = "NEWS_LATEST_API", resource = "NEWS_API", level = SecurityAudit.SecurityLevel.INFO)
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<ApiResponse<Newses>> getLatestNews(
            @RequestParam(defaultValue = "10") int limit,
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            Newses newses = newsQueryUseCase.getLatestNews(limit);
            return ResponseEntity.ok(ApiResponse.success(newses, "최신 뉴스 조회에 성공했습니다"));
        } catch (SystemException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("최신 뉴스 조회 중 오류가 발생했습니다.", 500));
        }
    }

    @GetMapping("/personalized")
    @LogExecution("개인화 뉴스 조회 API")
    @PerformanceMonitor(threshold = 2000, metricName = "api.news.personalized")
    @SecurityAudit(action = "NEWS_PERSONALIZED_API", resource = "NEWS_API", level = SecurityAudit.SecurityLevel.INFO)
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<ApiResponse<Newses>> getPersonalizedNews(
            @RequestParam(defaultValue = "10") int limit,
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            Newses newses = newsQueryUseCase.getPersonalizedNews(currentUser.getEmail(), limit);
            return ResponseEntity.ok(ApiResponse.success(newses, "개인화 뉴스 조회에 성공했습니다"));
        } catch (SystemException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("개인화 뉴스 조회 중 오류가 발생했습니다.", 500));
        }
    }
}