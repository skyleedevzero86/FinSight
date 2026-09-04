package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.global.security.SecurityAccess;
import com.sleekydz86.finsight.core.popup.domain.port.in.dto.PopupItemCreateRequest;
import com.sleekydz86.finsight.core.popup.domain.port.in.dto.PopupItemResponse;
import com.sleekydz86.finsight.core.popup.domain.port.in.dto.PopupItemUpdateRequest;
import com.sleekydz86.finsight.core.popup.service.PopupItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(
        name = "팝업 CMS",
        description = "레이어 팝업 CRUD. 공개 노출 목록(GET, activeOnly=true)은 비인증 가능. 등록·수정·삭제·전체목록은 ADMIN/MANAGER.")
@RestController
@RequestMapping("/api/v1/popup/items")
public class PopupItemController {

    private final PopupItemService popupItemService;

    public PopupItemController(PopupItemService popupItemService) {
        this.popupItemService = popupItemService;
    }

    @Operation(
            summary = "팝업 목록 조회",
            description = "activeOnly=true(기본): 활성(Y) 항목만 공개 조회(비인증 가능). activeOnly=false: ADMIN/MANAGER만 전체 조회.")
    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<PopupItemResponse>>> list(
            @Parameter(description = "사이트·테넌트 구분 (선택)")
            @RequestParam(required = false) String domainId,
            @Parameter(description = "true면 활성(Y)만, false면 전체(관리자)")
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @Parameter(description = "페이지 (0부터)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (1~100)")
            @RequestParam(defaultValue = "20") int size) {
        if (!activeOnly) {
            SecurityAccess.requireAdminOrManager();
        }
        int p = Math.max(0, page);
        int s = Math.min(100, Math.max(1, size));
        log.info("팝업 목록 조회 - domainId={}, activeOnly={}, page={}, size={}", domainId, activeOnly, p, s);
        return ResponseEntity.ok(
                ApiResponse.success(popupItemService.list(domainId, activeOnly, p, s), "팝업 목록을 조회했습니다"));
    }

    @Operation(summary = "팝업 상세 조회", description = "관리자용 상세. ADMIN/MANAGER 인증 필요.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<PopupItemResponse>> get(@PathVariable String id) {
        log.info("팝업 상세 조회 - id={}", id);
        return ResponseEntity.ok(ApiResponse.success(popupItemService.get(id), "팝업 상세를 조회했습니다"));
    }

    @Operation(summary = "팝업 등록", description = "팝업 항목 등록. ADMIN/MANAGER.")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<PopupItemResponse>> create(@Valid @RequestBody PopupItemCreateRequest request) {
        log.info("팝업 등록 - title={}", request.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(popupItemService.create(request), "팝업을 등록했습니다"));
    }

    @Operation(summary = "팝업 수정", description = "팝업 항목 수정. ADMIN/MANAGER.")
    @SecurityRequirement(name = "BearerAuth")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<PopupItemResponse>> update(
            @PathVariable String id, @Valid @RequestBody PopupItemUpdateRequest request) {
        log.info("팝업 수정 - id={}", id);
        return ResponseEntity.ok(ApiResponse.success(popupItemService.update(id, request), "팝업을 수정했습니다"));
    }

    @Operation(summary = "팝업 삭제", description = "팝업 항목 삭제. ADMIN/MANAGER.")
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        log.info("팝업 삭제 - id={}", id);
        popupItemService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "팝업을 삭제했습니다"));
    }
}
