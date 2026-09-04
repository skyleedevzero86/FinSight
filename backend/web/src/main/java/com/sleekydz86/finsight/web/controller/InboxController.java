package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.global.annotation.CurrentUser;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.inbox.domain.port.in.dto.InboxItemResponse;
import com.sleekydz86.finsight.core.inbox.domain.port.in.dto.InboxSettingsResponse;
import com.sleekydz86.finsight.core.inbox.domain.port.in.dto.InboxSettingsUpdateRequest;
import com.sleekydz86.finsight.core.inbox.domain.port.in.dto.InboxUnreadCountResponse;
import com.sleekydz86.finsight.core.inbox.service.InboxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(
        name = "인앱 알림함",
        description = "로그인 사용자(일반·관리자) 인앱 알림. 목록·미읽음·읽음·삭제·수신설정. "
                + "카테고리: YOUTUBE, NEWS, COMMENT, QNA, WATCHLIST, ADMIN")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/inbox")
@RequiredArgsConstructor
public class InboxController {

    private final InboxService inboxService;

    @Operation(summary = "알림 목록 조회", description = "페이지 기반 목록. 프론트 무한스크롤용 hasNext 제공. unreadOnly=true면 미읽음만.")
    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<InboxItemResponse>>> list(
            @CurrentUser AuthenticatedUser currentUser,
            @Parameter(description = "미읽음만") @RequestParam(defaultValue = "false") boolean unreadOnly,
            @Parameter(description = "페이지 (0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (1~50)") @RequestParam(defaultValue = "20") int size) {
        log.info("인앱 알림 목록 조회 - userId={}, unreadOnly={}, page={}, size={}",
                currentUser.getId(), unreadOnly, page, size);
        return ResponseEntity.ok(ApiResponse.success(
                inboxService.list(currentUser.getId(), unreadOnly, page, size),
                "알림 목록을 조회했습니다"));
    }

    @Operation(summary = "미읽음 알림 수", description = "헤더 벨 배지용 미읽음 카운트")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<InboxUnreadCountResponse>> unreadCount(
            @CurrentUser AuthenticatedUser currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                inboxService.unreadCount(currentUser.getId()),
                "미읽음 수를 조회했습니다"));
    }

    @Operation(summary = "알림 읽음 처리", description = "단건 읽음")
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<InboxItemResponse>> markRead(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable Long notificationId) {
        log.info("인앱 알림 읽음 - userId={}, id={}", currentUser.getId(), notificationId);
        return ResponseEntity.ok(ApiResponse.success(
                inboxService.markRead(currentUser.getId(), notificationId),
                "알림을 읽음 처리했습니다"));
    }

    @Operation(summary = "모두 읽음", description = "수신자의 미읽음 알림 전부 읽음")
    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<InboxUnreadCountResponse>> markAllRead(
            @CurrentUser AuthenticatedUser currentUser) {
        int updated = inboxService.markAllRead(currentUser.getId());
        log.info("인앱 알림 모두 읽음 - userId={}, count={}", currentUser.getId(), updated);
        return ResponseEntity.ok(ApiResponse.success(
                new InboxUnreadCountResponse(0),
                "모든 알림을 읽음 처리했습니다"));
    }

    @Operation(summary = "알림 단건 삭제", description = "소프트 삭제")
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> deleteOne(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable Long notificationId) {
        inboxService.deleteOne(currentUser.getId(), notificationId);
        return ResponseEntity.ok(ApiResponse.success(null, "알림을 삭제했습니다"));
    }

    @Operation(summary = "알림 모두 삭제", description = "수신자 알림 전부 소프트 삭제")
    @DeleteMapping
    public ResponseEntity<ApiResponse<InboxUnreadCountResponse>> deleteAll(
            @CurrentUser AuthenticatedUser currentUser) {
        int updated = inboxService.deleteAll(currentUser.getId());
        log.info("인앱 알림 모두 삭제 - userId={}, count={}", currentUser.getId(), updated);
        return ResponseEntity.ok(ApiResponse.success(
                new InboxUnreadCountResponse(0),
                "모든 알림을 삭제했습니다"));
    }

    @Operation(summary = "알림 수신 설정 조회", description = "유튜브·뉴스·댓글·QnA 수신 여부. 관심종목은 /users/watchlist")
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<InboxSettingsResponse>> getSettings(
            @CurrentUser AuthenticatedUser currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                inboxService.getSettings(currentUser.getId()),
                "알림 수신 설정을 조회했습니다"));
    }

    @Operation(summary = "알림 수신 설정 변경", description = "유튜브·뉴스·댓글·QnA 수신 여부 저장")
    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<InboxSettingsResponse>> updateSettings(
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody InboxSettingsUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                inboxService.updateSettings(currentUser.getId(), request),
                "알림 수신 설정을 저장했습니다"));
    }
}
