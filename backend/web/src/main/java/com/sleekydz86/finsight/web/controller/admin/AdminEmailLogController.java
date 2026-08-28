package com.sleekydz86.finsight.web.controller.admin;

import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.notification.domain.EmailActorType;
import com.sleekydz86.finsight.core.notification.domain.EmailMailPurpose;
import com.sleekydz86.finsight.core.notification.domain.EmailStatus;
import com.sleekydz86.finsight.core.notification.domain.port.in.EmailLogQueryUseCase;
import com.sleekydz86.finsight.core.notification.domain.port.in.dto.EmailLogSearchCriteria;
import com.sleekydz86.finsight.core.notification.domain.port.out.dto.EmailLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/email-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@Tag(name = "Admin Email Logs", description = "관리자 메일 발송 이력")
public class AdminEmailLogController {

    private final EmailLogQueryUseCase emailLogQueryUseCase;

    @GetMapping
    @Operation(summary = "메일 발송 이력 목록")
    public ResponseEntity<ApiResponse<PaginationResponse<EmailLogResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) EmailStatus status,
            @RequestParam(required = false) EmailMailPurpose purpose,
            @RequestParam(required = false) EmailActorType actorType,
            @RequestParam(required = false) String requestIp,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        log.info("메일 발송 이력 조회: page={}, size={}, keyword={}, status={}, purpose={}, actorType={}",
                page, size, keyword, status, purpose, actorType);
        EmailLogSearchCriteria criteria = new EmailLogSearchCriteria(
                keyword, status, purpose, actorType, requestIp, from, to);
        PaginationResponse<EmailLogResponse> response = PaginationResponse.from(
                emailLogQueryUseCase.search(
                        criteria,
                        PageRequest.of(
                                Math.max(page, 0),
                                Math.min(Math.max(size, 1), 100),
                                Sort.by(Sort.Direction.DESC, "createdAt"))));
        return ResponseEntity.ok(ApiResponse.success(response, "메일 발송 이력을 조회했습니다"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "메일 발송 이력 단건")
    public ResponseEntity<ApiResponse<EmailLogResponse>> getOne(@PathVariable Long id) {
        try {
            return emailLogQueryUseCase.findById(id)
                    .map(item -> ResponseEntity.ok(ApiResponse.success(item)))
                    .orElseGet(() -> ResponseEntity.badRequest()
                            .body(ApiResponse.error("메일 발송 이력을 찾을 수 없습니다.", 404)));
        } catch (Exception e) {
            log.error("메일 발송 이력 단건 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), 400));
        }
    }
}
