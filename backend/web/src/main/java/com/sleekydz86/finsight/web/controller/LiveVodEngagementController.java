package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.global.annotation.CurrentUser;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.CommentCreateRequest;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.CommentPageResponse;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.CommentResponse;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.EngagementSummary;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.FavoriteToggleResponse;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.LiveVodMetaResponse;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.ReactionToggleRequest;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.ReactionToggleResponse;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.ReplyPageResponse;
import com.sleekydz86.finsight.core.media.livevod.service.LiveVodEngagementService;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.YoutubeMediaQueryUseCase;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.LiveVodFeedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "LIVE/VOD", description = "LIVE/VOD 피드·참여(즐겨찾기·반응·댓글) API")
@RestController
@RequestMapping("/api/v1/media")
public class LiveVodEngagementController {

    private final YoutubeMediaQueryUseCase youtubeMediaQueryUseCase;
    private final LiveVodEngagementService liveVodEngagementService;

    public LiveVodEngagementController(
            YoutubeMediaQueryUseCase youtubeMediaQueryUseCase,
            LiveVodEngagementService liveVodEngagementService) {
        this.youtubeMediaQueryUseCase = youtubeMediaQueryUseCase;
        this.liveVodEngagementService = liveVodEngagementService;
    }

    @Operation(summary = "LIVE/VOD 피드 조회", description = "탭별 LIVE/VOD 피드를 조회합니다.", security = {})
    @GetMapping("/videos/live-vod")
    public ResponseEntity<ApiResponse<LiveVodFeedResponse>> getLiveVodFeed(
            @RequestParam(required = false, defaultValue = "ALL") String tab) {
        LiveVodFeedResponse feed = youtubeMediaQueryUseCase.getLiveVodFeed(tab);
        LiveVodFeedResponse enriched = liveVodEngagementService.enrichFeed(feed);
        return ResponseEntity.ok(ApiResponse.success(enriched, "LIVE/VOD 피드를 성공적으로 조회했습니다."));
    }

    @Operation(summary = "LIVE/VOD 메타 조회", description = "영상 메타 정보를 조회합니다.", security = {})
    @GetMapping("/live-vod/{videoId}/meta")
    public ResponseEntity<ApiResponse<LiveVodMetaResponse>> getMeta(@PathVariable String videoId) {
        return ResponseEntity.ok(ApiResponse.success(
                liveVodEngagementService.getMeta(videoId),
                "영상 정보를 조회했습니다."));
    }

    @Operation(summary = "LIVE/VOD 참여 정보 조회", description = "즐겨찾기·반응 등 참여 정보를 조회합니다. 로그인 시 내 참여 상태가 포함됩니다.", security = {})
    @GetMapping("/live-vod/{videoId}/engagement")
    public ResponseEntity<ApiResponse<EngagementSummary>> getEngagement(
            @PathVariable String videoId,
            @CurrentUser(required = false) AuthenticatedUser currentUser) {
        String email = currentUser == null ? null : currentUser.getEmail();
        return ResponseEntity.ok(ApiResponse.success(
                liveVodEngagementService.getEngagement(videoId, email),
                "참여 정보를 조회했습니다."));
    }

    @Operation(summary = "즐겨찾기 토글", description = "영상 즐겨찾기를 추가하거나 해제합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/live-vod/{videoId}/favorite")
    public ResponseEntity<ApiResponse<FavoriteToggleResponse>> toggleFavorite(
            @PathVariable String videoId,
            @CurrentUser AuthenticatedUser currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                liveVodEngagementService.toggleFavorite(videoId, currentUser.getEmail()),
                "즐겨찾기를 반영했습니다."));
    }

    @Operation(summary = "영상 반응 토글", description = "영상에 대한 반응을 추가하거나 해제합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/live-vod/{videoId}/reaction")
    public ResponseEntity<ApiResponse<ReactionToggleResponse>> toggleReaction(
            @PathVariable String videoId,
            @RequestBody ReactionToggleRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        String reaction = request == null ? null : request.reaction();
        return ResponseEntity.ok(ApiResponse.success(
                liveVodEngagementService.toggleReaction(videoId, currentUser.getEmail(), reaction),
                "반응을 반영했습니다."));
    }

    @Operation(summary = "댓글 목록 조회", description = "영상 댓글 목록을 조회합니다. 로그인 시 내 반응 상태가 포함됩니다.", security = {})
    @GetMapping("/live-vod/{videoId}/comments")
    public ResponseEntity<ApiResponse<CommentPageResponse>> listComments(
            @PathVariable String videoId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "15") int size,
            @CurrentUser(required = false) AuthenticatedUser currentUser) {
        String email = currentUser == null ? null : currentUser.getEmail();
        return ResponseEntity.ok(ApiResponse.success(
                liveVodEngagementService.listComments(videoId, page, size, email),
                "댓글 목록을 조회했습니다."));
    }

    @Operation(summary = "대댓글 목록 조회", description = "댓글의 대댓글 목록을 조회합니다. 로그인 시 내 반응 상태가 포함됩니다.", security = {})
    @GetMapping("/live-vod/{videoId}/comments/{parentId}/replies")
    public ResponseEntity<ApiResponse<ReplyPageResponse>> listReplies(
            @PathVariable String videoId,
            @PathVariable Long parentId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "5") int size,
            @CurrentUser(required = false) AuthenticatedUser currentUser) {
        String email = currentUser == null ? null : currentUser.getEmail();
        return ResponseEntity.ok(ApiResponse.success(
                liveVodEngagementService.listReplies(videoId, parentId, page, size, email),
                "대댓글 목록을 조회했습니다."));
    }

    @Operation(summary = "댓글 반응 토글", description = "댓글에 대한 반응을 추가하거나 해제합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/live-vod/{videoId}/comments/{commentId}/reaction")
    public ResponseEntity<ApiResponse<ReactionToggleResponse>> toggleCommentReaction(
            @PathVariable String videoId,
            @PathVariable Long commentId,
            @RequestBody ReactionToggleRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        String reaction = request == null ? null : request.reaction();
        return ResponseEntity.ok(ApiResponse.success(
                liveVodEngagementService.toggleCommentReaction(commentId, currentUser.getEmail(), reaction),
                "댓글 반응을 반영했습니다."));
    }

    @Operation(summary = "댓글 등록", description = "영상에 댓글 또는 대댓글을 등록합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/live-vod/{videoId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable String videoId,
            @RequestBody CommentCreateRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        String nickname = currentUser.getNickname() != null ? currentUser.getNickname() : currentUser.getEmail();
        return ResponseEntity.ok(ApiResponse.success(
                liveVodEngagementService.createComment(
                        videoId, currentUser.getEmail(), nickname, request),
                "댓글을 등록했습니다."));
    }
}
