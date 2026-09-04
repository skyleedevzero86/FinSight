package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.ulink.domain.port.in.dto.UlinkItemCreateRequest;
import com.sleekydz86.finsight.core.ulink.domain.port.in.dto.UlinkItemResponse;
import com.sleekydz86.finsight.core.ulink.domain.port.in.dto.UlinkItemUpdateRequest;
import com.sleekydz86.finsight.core.ulink.service.UlinkItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "유링크 CMS", description = "통합링크 항목 CRUD API")
@RestController
@RequestMapping("/api/v1/ulink/items")
public class UlinkItemController {

    private final UlinkItemService ulinkItemService;

    public UlinkItemController(UlinkItemService ulinkItemService) {
        this.ulinkItemService = ulinkItemService;
    }

    @Operation(summary = "통합링크 목록 조회", description = "통합링크 항목 목록을 조회합니다. 스태프 인증이 적용될 수 있습니다.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<UlinkItemResponse>>> list(
            @RequestParam(required = false) String domainId,
            @RequestParam(required = false) String sectionCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int p = Math.max(0, page);
        int s = Math.min(100, Math.max(1, size));
        PaginationResponse<UlinkItemResponse> data =
                ulinkItemService.list(domainId, sectionCode, p, s);
        return ResponseEntity.ok(ApiResponse.success(data, "통합링크 목록을 조회했습니다"));
    }

    @Operation(summary = "통합링크 상세 조회", description = "통합링크 항목 상세를 조회합니다. 스태프 인증이 적용될 수 있습니다.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UlinkItemResponse>> get(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(ulinkItemService.get(id), "통합링크 상세를 조회했습니다"));
    }

    @Operation(summary = "통합링크 등록", description = "통합링크 항목을 등록합니다. 스태프 인증이 적용될 수 있습니다.")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping
    public ResponseEntity<ApiResponse<UlinkItemResponse>> create(
            @Valid @RequestBody UlinkItemCreateRequest request) {
        UlinkItemResponse created = ulinkItemService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "통합링크 항목을 등록했습니다"));
    }

    @Operation(summary = "통합링크 수정", description = "통합링크 항목을 수정합니다. 스태프 인증이 적용될 수 있습니다.")
    @SecurityRequirement(name = "BearerAuth")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UlinkItemResponse>> update(
            @PathVariable String id, @Valid @RequestBody UlinkItemUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(ulinkItemService.update(id, request), "통합링크 항목을 수정했습니다"));
    }

    @Operation(summary = "통합링크 삭제", description = "통합링크 항목을 삭제합니다. 스태프 인증이 적용될 수 있습니다.")
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        ulinkItemService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "통합링크 항목을 삭제했습니다"));
    }
}
