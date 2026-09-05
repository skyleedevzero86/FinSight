package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.ulink.domain.port.in.dto.UlinkItemCreateRequest;
import com.sleekydz86.finsight.core.ulink.domain.port.in.dto.UlinkItemResponse;
import com.sleekydz86.finsight.core.ulink.domain.port.in.dto.UlinkItemUpdateRequest;
import com.sleekydz86.finsight.core.ulink.service.UlinkItemService;
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
        name = "유링크 CMS",
        description = "통합링크 CRUD. 목록 GET은 공개(푸터·메뉴 노출용). 상세·등록·수정·삭제는 ADMIN/MANAGER. "
                + "sectionCode 예: FOOTER_SERVICE, FOOTER_POLICY, FOOTER_SOCIAL")
@RestController
@RequestMapping("/api/v1/ulink/items")
public class UlinkItemController {

    private final UlinkItemService ulinkItemService;

    public UlinkItemController(UlinkItemService ulinkItemService) {
        this.ulinkItemService = ulinkItemService;
    }

    @Operation(
            summary = "통합링크 목록 조회",
            description = "공개 API. sectionCode로 푸터/메뉴 구역을 필터합니다. "
                    + "예: FOOTER_SERVICE | FOOTER_POLICY | FOOTER_SOCIAL")
    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<UlinkItemResponse>>> list(
            @Parameter(description = "사이트·테넌트 구분 (선택)")
            @RequestParam(required = false) String domainId,
            @Parameter(description = "구역 코드 (선택)", example = "FOOTER_SERVICE")
            @RequestParam(required = false) String sectionCode,
            @Parameter(description = "오픈(Y) 항목만 조회")
            @RequestParam(defaultValue = "false") boolean openOnly,
            @Parameter(description = "페이지 (0부터)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (1~100)")
            @RequestParam(defaultValue = "20") int size) {
        int p = Math.max(0, page);
        int s = Math.min(100, Math.max(1, size));
        log.info("통합링크 목록 조회 - domainId={}, sectionCode={}, openOnly={}, page={}, size={}",
                domainId, sectionCode, openOnly, p, s);
        PaginationResponse<UlinkItemResponse> data =
                ulinkItemService.list(domainId, sectionCode, openOnly, p, s);
        return ResponseEntity.ok(ApiResponse.success(data, "통합링크 목록을 조회했습니다"));
    }

    @Operation(summary = "통합링크 상세 조회", description = "관리자용 상세. ADMIN/MANAGER.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<UlinkItemResponse>> get(@PathVariable String id) {
        log.info("통합링크 상세 조회 - id={}", id);
        return ResponseEntity.ok(ApiResponse.success(ulinkItemService.get(id), "통합링크 상세를 조회했습니다"));
    }

    @Operation(summary = "통합링크 등록", description = "통합링크 항목 등록. ADMIN/MANAGER.")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<UlinkItemResponse>> create(
            @Valid @RequestBody UlinkItemCreateRequest request) {
        log.info("통합링크 등록 - sectionCode={}, linkName={}", request.getSectionCode(), request.getLinkName());
        UlinkItemResponse created = ulinkItemService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "통합링크 항목을 등록했습니다"));
    }

    @Operation(summary = "통합링크 수정", description = "통합링크 항목 수정. ADMIN/MANAGER.")
    @SecurityRequirement(name = "BearerAuth")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<UlinkItemResponse>> update(
            @PathVariable String id, @Valid @RequestBody UlinkItemUpdateRequest request) {
        log.info("통합링크 수정 - id={}", id);
        return ResponseEntity.ok(ApiResponse.success(ulinkItemService.update(id, request), "통합링크 항목을 수정했습니다"));
    }

    @Operation(summary = "통합링크 삭제", description = "통합링크 항목 삭제. ADMIN/MANAGER.")
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        log.info("통합링크 삭제 - id={}", id);
        ulinkItemService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "통합링크 항목을 삭제했습니다"));
    }
}
