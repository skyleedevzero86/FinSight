package com.sleekydz86.finsight.web.controller.admin;

import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.inbox.domain.port.in.dto.InboxBroadcastRequest;
import com.sleekydz86.finsight.core.inbox.domain.port.in.dto.InboxBroadcastResponse;
import com.sleekydz86.finsight.core.inbox.domain.port.in.dto.InboxItemResponse;
import com.sleekydz86.finsight.core.inbox.service.InboxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(
        name = "관리자 인앱 알림",
        description = "ADMIN/MANAGER 인앱 알림 일괄 등록·최근 발송 목록. "
                + "등록된 알림은 대상 사용자 헤더 알림함에 표시됩니다.")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/admin/inbox")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AdminInboxController {

    private final InboxService inboxService;

    @Operation(summary = "최근 인앱 알림 목록", description = "전체 사용자 대상 최근 알림(관리 모니터링용)")
    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<InboxItemResponse>>> list(
            @Parameter(description = "페이지 (0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (1~50)") @RequestParam(defaultValue = "20") int size) {
        log.info("관리자 인앱 알림 목록 - page={}, size={}", page, size);
        return ResponseEntity.ok(ApiResponse.success(
                inboxService.adminList(page, size),
                "관리자 알림 목록을 조회했습니다"));
    }

    @Operation(
            summary = "인앱 알림 일괄 등록",
            description = "allUsers=true 전체, adminsOnly=true 관리자만, 아니면 userIds. "
                    + "수신 설정(유튜브/뉴스/댓글/QnA)이 꺼진 사용자는 해당 카테고리에서 제외. ADMIN/WATCHLIST는 설정과 무관(관심종목은 뉴스 설정 따름).")
    @PostMapping("/broadcast")
    public ResponseEntity<ApiResponse<InboxBroadcastResponse>> broadcast(
            @Valid @RequestBody InboxBroadcastRequest request) {
        log.info("관리자 인앱 알림 등록 - category={}, allUsers={}, adminsOnly={}",
                request.category(), request.allUsers(), request.adminsOnly());
        InboxBroadcastResponse result = inboxService.broadcast(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "인앱 알림을 등록했습니다"));
    }
}
