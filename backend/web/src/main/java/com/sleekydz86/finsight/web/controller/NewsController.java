package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsQueryUseCase;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsDetailResponse;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsSearchRequest;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsStatisticsPersistencePort;
import com.sleekydz86.finsight.core.news.domain.NewsStatistics;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.global.NewsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/news")
@CrossOrigin(origins = "*")
public class NewsController {

    private static final Logger log = LoggerFactory.getLogger(NewsController.class);

    private final NewsQueryUseCase newsQueryUseCase;
    private final NewsStatisticsPersistencePort newsStatisticsPersistencePort;

    public NewsController(NewsQueryUseCase newsQueryUseCase,
                          NewsStatisticsPersistencePort newsStatisticsPersistencePort) {
        this.newsQueryUseCase = newsQueryUseCase;
        this.newsStatisticsPersistencePort = newsStatisticsPersistencePort;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PaginationResponse<Newses>>> searchNews(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) List<String> providers,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) String sentiment,
            @RequestParam(defaultValue = "CARD") String listType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.info("Searching news with keyword: {}, page: {}, size: {}", keyword, page, size);

        LocalDateTime start = null;
        LocalDateTime end = null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        if (startDate != null && !startDate.isEmpty()) {
            start = LocalDateTime.parse(startDate + "T00:00:00");
        }
        if (endDate != null && !endDate.isEmpty()) {
            end = LocalDateTime.parse(endDate + "T23:59:59");
        }
        SentimentType sentimentType = null;
        if (sentiment != null && !sentiment.isEmpty()) {
            try {
                sentimentType = SentimentType.valueOf(sentiment.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid sentiment type: {}", sentiment);
            }
        }
        List<TargetCategory> targetCategories = null;
        if (categories != null && !categories.isEmpty()) {
            targetCategories = categories.stream()
                    .map(cat -> {
                        try {
                            return TargetCategory.valueOf(cat.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            log.warn("Invalid category: {}", cat);
                            return null;
                        }
                    })
                    .filter(cat -> cat != null)
                    .collect(Collectors.toList());
        }
        List<NewsProvider> newsProviders = null;
        if (providers != null && !providers.isEmpty()) {
            newsProviders = providers.stream()
                    .map(prov -> {
                        try {
                            return NewsProvider.valueOf(prov.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            log.warn("Invalid provider: {}", prov);
                            return null;
                        }
                    })
                    .filter(prov -> prov != null)
                    .collect(Collectors.toList());
        }

        NewsSearchRequest request = new NewsSearchRequest(
                start,
                end,
                sentimentType,
                keyword,
                targetCategories,
                newsProviders,
                page,
                size
        );

        PaginationResponse<Newses> result = newsQueryUseCase.searchNews(request);

        return ResponseEntity.ok(ApiResponse.success(result, "뉴스 검색이 완료되었습니다"));
    }

    @GetMapping("/{newsId}")
    public ResponseEntity<ApiResponse<NewsDetailResponse>> getNewsDetail(@PathVariable Long newsId) {
        log.info("Getting news detail for ID: {}", newsId);

        NewsDetailResponse response = newsQueryUseCase.getNewsDetail(newsId);

        return ResponseEntity.ok(ApiResponse.success(response, "뉴스 상세 정보를 조회했습니다"));
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<Newses>> getPopularNews(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Getting popular news, limit: {}", limit);

        Newses newses = newsQueryUseCase.getPopularNews(limit);

        return ResponseEntity.ok(ApiResponse.success(newses, "인기 뉴스를 조회했습니다"));
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<Newses>> getLatestNews(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Getting latest news, limit: {}", limit);

        Newses newses = newsQueryUseCase.getLatestNews(limit);

        return ResponseEntity.ok(ApiResponse.success(newses, "최신 뉴스를 조회했습니다"));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<Newses>> getNewsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Getting news by category: {}, limit: {}", category, limit);

        Newses newses = newsQueryUseCase.getNewsByCategory(category, limit);

        return ResponseEntity.ok(ApiResponse.success(newses, "카테고리별 뉴스를 조회했습니다"));
    }

    @PostMapping("/{newsId}/like")
    public ResponseEntity<ApiResponse<NewsStatistics>> likeNews(@PathVariable Long newsId) {
        log.info("Liking news: {}", newsId);

        NewsStatistics statistics = newsStatisticsPersistencePort.incrementLikeCount(newsId);

        return ResponseEntity.ok(ApiResponse.success(statistics, "뉴스에 좋아요를 눌렀습니다"));
    }

    @PostMapping("/{newsId}/dislike")
    public ResponseEntity<ApiResponse<NewsStatistics>> dislikeNews(@PathVariable Long newsId) {
        log.info("Disliking news: {}", newsId);

        NewsStatistics statistics = newsStatisticsPersistencePort.incrementDislikeCount(newsId);

        return ResponseEntity.ok(ApiResponse.success(statistics, "뉴스에 싫어요를 눌렀습니다"));
    }
}