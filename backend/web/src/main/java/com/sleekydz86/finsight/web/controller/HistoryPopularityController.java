package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.history.domain.dto.HistoryPopularityDtos.PopularityRequest;
import com.sleekydz86.finsight.core.history.domain.dto.HistoryPopularityDtos.PopularityResponse;
import com.sleekydz86.finsight.core.history.service.HistoryPopularityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "시청 기록", description = "최근 본 게시물 히스토리 보조 API")
@RestController
@RequestMapping({"/api/v1/media/history", "/api/v1/history"})
public class HistoryPopularityController {

    private final HistoryPopularityService historyPopularityService;

    public HistoryPopularityController(HistoryPopularityService historyPopularityService) {
        this.historyPopularityService = historyPopularityService;
    }

    @Operation(summary = "히스토리 인기 점수", description = "시청 기록 항목의 인기 점수를 일괄 조회합니다.", security = {})
    @PostMapping("/popularity")
    public ResponseEntity<ApiResponse<PopularityResponse>> popularity(@RequestBody PopularityRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                historyPopularityService.score(request),
                "히스토리 인기 점수를 조회했습니다."));
    }
}
