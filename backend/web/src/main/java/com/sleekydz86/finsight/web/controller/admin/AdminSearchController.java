package com.sleekydz86.finsight.web.controller.admin;

import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.exception.SystemException;
import com.sleekydz86.finsight.core.global.exception.ValidationException;
import com.sleekydz86.finsight.core.search.service.SearchAliasReindexer;
import com.sleekydz86.finsight.core.search.service.SearchAliasReindexer.SearchReindexReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/search")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AdminSearchController {

    private static final Logger log = LoggerFactory.getLogger(AdminSearchController.class);

    private final SearchAliasReindexer reindexer;

    public AdminSearchController(SearchAliasReindexer reindexer) {
        this.reindexer = reindexer;
    }

    @PostMapping("/reindex")
    public ResponseEntity<ApiResponse<SearchReindexReport>> reindex() {
        try {
            return ResponseEntity.ok(ApiResponse.success(reindexer.rebuild(), "검색 색인을 교체했습니다"));
        } catch (ValidationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("검색 재색인 실패", exception);
            throw new SystemException("검색 색인을 교체하지 못했습니다.", "SEARCH_REINDEX_ERROR", exception);
        }
    }
}
