package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsCommandUseCase;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsQueryUseCase;
import com.sleekydz86.finsight.core.news.service.NewsAiAvailability;
import com.sleekydz86.finsight.core.news.service.NewsReactionService;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsDetailResponse;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsReactionResponse;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsQueryRequest;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsSearchRequest;
import com.sleekydz86.finsight.core.global.annotation.CurrentUser;
import com.sleekydz86.finsight.core.global.annotation.LogExecution;
import com.sleekydz86.finsight.core.global.annotation.PerformanceMonitor;
import com.sleekydz86.finsight.core.global.annotation.SecurityAudit;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.global.exception.BaseException;
import com.sleekydz86.finsight.core.global.exception.NewsNotFoundException;
import com.sleekydz86.finsight.core.global.exception.SystemException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.concurrent.CompletableFuture;

@Tag(name = "뉴스", description = "뉴스 조회·검색·스크랩·개인화 API")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/news")
public class NewsController {

    private final NewsCommandUseCase newsCommandUseCase;
    private final NewsQueryUseCase newsQueryUseCase;
    private final NewsAiAvailability newsAiAvailability;
    private final NewsReactionService newsReactionService;

    public NewsController(NewsCommandUseCase newsCommandUseCase,
                          NewsQueryUseCase newsQueryUseCase,
                          NewsAiAvailability newsAiAvailability,
                          NewsReactionService newsReactionService) {
        this.newsCommandUseCase = newsCommandUseCase;
        this.newsQueryUseCase = newsQueryUseCase;
        this.newsAiAvailability = newsAiAvailability;
        this.newsReactionService = newsReactionService;
    }

    @Operation(summary = "뉴스 스크래핑", description = "뉴스를 스크래핑합니다.")
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

    @Operation(summary = "뉴스 반응 조회", description = "뉴스 좋아요·싫어요 수를 조회합니다. 로그인 시 내 반응이 포함됩니다.", security = {})
    @GetMapping("/{newsId}/reactions")
    public ResponseEntity<ApiResponse<NewsReactionResponse>> getNewsReactions(
            @PathVariable Long newsId,
            @CurrentUser(required = false) AuthenticatedUser currentUser) {
        try {
            String email = currentUser == null ? null : currentUser.getEmail();
            return ResponseEntity.ok(ApiResponse.success(
                    newsReactionService.view(newsId, email),
                    "뉴스 반응을 조회했습니다."));
        } catch (BaseException e) {
            return ResponseEntity.status(e.getHttpStatus()).body(ApiResponse.error(e.getMessage(), e.getHttpStatus()));
        }
    }

    @Operation(summary = "뉴스 반응 변경", description = "좋아요 또는 싫어요를 추가·변경·해제합니다.")
    @PostMapping("/{newsId}/reactions")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<ApiResponse<NewsReactionResponse>> toggleNewsReaction(
            @PathVariable Long newsId,
            @RequestBody NewsReactionRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            String reaction = request == null ? null : request.reaction();
            return ResponseEntity.ok(ApiResponse.success(
                    newsReactionService.toggle(newsId, currentUser.getEmail(), reaction),
                    "뉴스 반응을 반영했습니다."));
        } catch (BaseException e) {
            return ResponseEntity.status(e.getHttpStatus()).body(ApiResponse.error(e.getMessage(), e.getHttpStatus()));
        }
    }

    @Operation(summary = "뉴스 상세 조회", description = "뉴스 ID로 상세 정보를 조회합니다.")
    @GetMapping("/{newsId}")
    @LogExecution("뉴스 상세 조회 API")
    @PerformanceMonitor(threshold = 1000, metricName = "api.news.detail")
    @SecurityAudit(action = "NEWS_DETAIL_API", resource = "NEWS_API", level = SecurityAudit.SecurityLevel.INFO)
    public ResponseEntity<ApiResponse<NewsDetailResponse>> getNewsDetail(@PathVariable Long newsId) {
        try {
            NewsDetailResponse news = newsQueryUseCase.getNewsDetail(newsId);
            return ResponseEntity.ok(ApiResponse.success(news, "뉴스 상세 조회에 성공했습니다"));
        } catch (NewsNotFoundException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage(), 404));
        } catch (SystemException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("관리자에게 문의주세요.", 500));
        }
    }

    @Operation(summary = "뉴스 목록 조회", description = "필터 조건에 따라 뉴스 목록을 조회합니다.")
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

    @Operation(summary = "뉴스 검색", description = "키워드와 조건으로 뉴스를 검색합니다.")
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

    @Operation(summary = "인기 뉴스 조회", description = "인기 뉴스 목록을 조회합니다.")
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

    @Operation(summary = "뉴스 AI 상태", description = "AI 키와 연결 상태를 확인합니다.")
    @GetMapping("/ai-status")
    public ResponseEntity<ApiResponse<NewsAiAvailability.Status>> newsAiStatus() {
        NewsAiAvailability.Status status = newsAiAvailability.status();
        return ResponseEntity.ok(ApiResponse.success(status, status.available() ? "뉴스 AI를 사용할 수 있습니다" : status.message()));
    }

    @Operation(summary = "최신 뉴스 조회", description = "최신 뉴스 목록을 조회합니다.")
    @GetMapping("/latest")
    @LogExecution("최신 뉴스 조회 API")
    @PerformanceMonitor(threshold = 1000, metricName = "api.news.latest")
    @SecurityAudit(action = "NEWS_LATEST_API", resource = "NEWS_API", level = SecurityAudit.SecurityLevel.INFO)
    public ResponseEntity<ApiResponse<Newses>> getLatestNews(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String provider) {
        try {
            Newses newses = provider == null || provider.isBlank()
                    ? newsQueryUseCase.getLatestNews(limit)
                    : newsQueryUseCase.getLatestNewsByProvider(provider, limit);
            return ResponseEntity.ok(ApiResponse.success(newses, "최신 뉴스 조회에 성공했습니다"));
        } catch (SystemException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("관리자에게 문의주세요.", 500));
        }
    }

    @Operation(summary = "개인화 뉴스 조회", description = "로그인한 사용자 맞춤 뉴스 목록을 조회합니다.")
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

    public record NewsReactionRequest(String reaction) {
    }
}
