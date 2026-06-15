package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.global.annotation.CurrentUser;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.YoutubeMediaAdminUseCase;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeAdminVideoSearchRequest;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeAiEnrichmentSummaryResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeImportSourceCreateRequest;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeImportSourceResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeManualImportRequest;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeSourceReviewRequest;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeSourceReviewResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeSyncSummaryResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeVideoDetailResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeVideoListResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeVideoPublishRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/media")
@PreAuthorize("hasRole('ADMIN')")
public class AdminYoutubeMediaController {

    private final YoutubeMediaAdminUseCase youtubeMediaAdminUseCase;

    public AdminYoutubeMediaController(YoutubeMediaAdminUseCase youtubeMediaAdminUseCase) {
        this.youtubeMediaAdminUseCase = youtubeMediaAdminUseCase;
    }

    @GetMapping("/videos")
    public ResponseEntity<ApiResponse<PaginationResponse<YoutubeVideoListResponse>>> getAdminVideos(
            @Valid YoutubeAdminVideoSearchRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        PaginationResponse<YoutubeVideoListResponse> response = youtubeMediaAdminUseCase.getAdminVideos(request);
        return ResponseEntity.ok(ApiResponse.success(response, "관리자 영상 목록을 성공적으로 조회했습니다."));
    }

    @GetMapping("/videos/{boardId}")
    public ResponseEntity<ApiResponse<YoutubeVideoDetailResponse>> getAdminVideoDetail(
            @PathVariable Long boardId,
            @CurrentUser AuthenticatedUser currentUser) {
        YoutubeVideoDetailResponse response = youtubeMediaAdminUseCase.getAdminVideoDetail(boardId);
        return ResponseEntity.ok(ApiResponse.success(response, "관리자 영상 상세 정보를 성공적으로 조회했습니다."));
    }

    @GetMapping("/sources")
    public ResponseEntity<ApiResponse<List<YoutubeImportSourceResponse>>> getImportSources(
            @CurrentUser AuthenticatedUser currentUser) {
        List<YoutubeImportSourceResponse> response = youtubeMediaAdminUseCase.getImportSources();
        return ResponseEntity.ok(ApiResponse.success(response, "YouTube 수집 소스 목록을 성공적으로 조회했습니다."));
    }

    @GetMapping("/sources/{sourceId}/review")
    public ResponseEntity<ApiResponse<YoutubeSourceReviewResponse>> getSourceReview(
            @PathVariable Long sourceId,
            @Valid YoutubeSourceReviewRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        YoutubeSourceReviewResponse response = youtubeMediaAdminUseCase.getSourceReview(sourceId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "YouTube 소스 검토 정보를 성공적으로 조회했습니다."));
    }

    @PostMapping("/sources")
    public ResponseEntity<ApiResponse<YoutubeImportSourceResponse>> createImportSource(
            @RequestBody @Valid YoutubeImportSourceCreateRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        YoutubeImportSourceResponse response = youtubeMediaAdminUseCase.createImportSource(currentUser.getEmail(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "YouTube 수집 소스를 성공적으로 생성했습니다."));
    }

    @PostMapping("/sources/{sourceId}/sync")
    public ResponseEntity<ApiResponse<YoutubeSyncSummaryResponse>> syncSource(
            @PathVariable Long sourceId,
            @CurrentUser AuthenticatedUser currentUser) {
        YoutubeSyncSummaryResponse response = youtubeMediaAdminUseCase.syncSource(sourceId);
        return ResponseEntity.ok(ApiResponse.success(response, "YouTube 소스를 성공적으로 동기화했습니다."));
    }

    @PostMapping("/import/manual")
    public ResponseEntity<ApiResponse<YoutubeSyncSummaryResponse>> importManualUrls(
            @RequestBody @Valid YoutubeManualImportRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        YoutubeSyncSummaryResponse response = youtubeMediaAdminUseCase.importManualUrls(currentUser.getEmail(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "YouTube 영상을 수동으로 성공적으로 가져왔습니다."));
    }

    @PostMapping("/import/sync")
    public ResponseEntity<ApiResponse<YoutubeSyncSummaryResponse>> syncAllSources(
            @CurrentUser AuthenticatedUser currentUser) {
        YoutubeSyncSummaryResponse response = youtubeMediaAdminUseCase.syncActiveSources();
        return ResponseEntity.ok(ApiResponse.success(response, "활성화된 모든 YouTube 소스를 성공적으로 동기화했습니다."));
    }

    @PostMapping("/import/enrich")
    public ResponseEntity<ApiResponse<YoutubeAiEnrichmentSummaryResponse>> enrichDraftVideos(
            @CurrentUser AuthenticatedUser currentUser) {
        YoutubeAiEnrichmentSummaryResponse response = youtubeMediaAdminUseCase.enrichPendingDraftVideos();
        return ResponseEntity.ok(ApiResponse.success(response, "대기 중인 초안 영상 보강을 성공적으로 완료했습니다."));
    }

    @PostMapping("/videos/{boardId}/publish")
    public ResponseEntity<ApiResponse<YoutubeVideoDetailResponse>> publishVideo(
            @PathVariable Long boardId,
            @RequestBody @Valid YoutubeVideoPublishRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        YoutubeVideoDetailResponse response = youtubeMediaAdminUseCase.publishVideo(boardId, currentUser.getEmail(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "YouTube 영상을 성공적으로 게시했습니다."));
    }

    @PostMapping("/videos/{boardId}/hide")
    public ResponseEntity<ApiResponse<YoutubeVideoDetailResponse>> hideVideo(
            @PathVariable Long boardId,
            @CurrentUser AuthenticatedUser currentUser) {
        YoutubeVideoDetailResponse response = youtubeMediaAdminUseCase.hideVideo(boardId);
        return ResponseEntity.ok(ApiResponse.success(response, "YouTube 영상을 성공적으로 숨김 처리했습니다."));
    }
}
