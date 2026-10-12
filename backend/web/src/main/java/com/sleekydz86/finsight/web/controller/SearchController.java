package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.global.annotation.CurrentUser;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
import com.sleekydz86.finsight.core.global.exception.SystemException;
import com.sleekydz86.finsight.core.search.domain.CatalogSearchResult;
import com.sleekydz86.finsight.core.search.service.CatalogSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final CatalogSearchService catalogSearchService;

    public SearchController(CatalogSearchService catalogSearchService) {
        this.catalogSearchService = catalogSearchService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CatalogSearchResult>> search(
            @CurrentUser(required = false) AuthenticatedUser currentUser,
            @RequestParam(name = "q", required = false) String query) {
        try {
            Long userId = currentUser == null ? null : currentUser.getId();
            String email = currentUser == null ? null : currentUser.getEmail();
            return ResponseEntity.ok(ApiResponse.success(
                    catalogSearchService.search(query, userId, email),
                    "검색 결과를 조회했습니다"));
        } catch (RuntimeException exception) {
            log.error("통합 검색 실패 queryLength={}", query == null ? 0 : query.length(), exception);
            throw new SystemException("검색 결과를 불러오지 못했습니다.", "CATALOG_SEARCH_ERROR", exception);
        }
    }
}
