package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.YoutubeMediaQueryUseCase;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeVideoDetailResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeVideoListResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeVideoSearchRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "YouTube 미디어", description = "게시된 YouTube 영상 공개 조회 API")
@RestController
@RequestMapping("/api/v1/media/videos")
public class YoutubeMediaController {

    private final YoutubeMediaQueryUseCase youtubeMediaQueryUseCase;

    public YoutubeMediaController(YoutubeMediaQueryUseCase youtubeMediaQueryUseCase) {
        this.youtubeMediaQueryUseCase = youtubeMediaQueryUseCase;
    }

    @Operation(summary = "게시된 YouTube 영상 목록 조회", description = "게시된 YouTube 영상 목록을 검색·조회합니다.", security = {})
    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<YoutubeVideoListResponse>>> getPublishedVideos(
            @Valid YoutubeVideoSearchRequest request) {
        PaginationResponse<YoutubeVideoListResponse> response = youtubeMediaQueryUseCase.getPublishedVideos(request);
        return ResponseEntity.ok(ApiResponse.success(response, "게시된 YouTube 영상 목록을 성공적으로 조회했습니다."));
    }

    @Operation(summary = "게시된 YouTube 영상 상세 조회", description = "게시된 YouTube 영상 상세 정보를 조회합니다.", security = {})
    @GetMapping("/{boardId}")
    public ResponseEntity<ApiResponse<YoutubeVideoDetailResponse>> getPublishedVideoDetail(
            @PathVariable Long boardId) {
        YoutubeVideoDetailResponse response = youtubeMediaQueryUseCase.getPublishedVideoDetail(boardId);
        return ResponseEntity.ok(ApiResponse.success(response, "게시된 YouTube 영상 상세 정보를 성공적으로 조회했습니다."));
    }
}
