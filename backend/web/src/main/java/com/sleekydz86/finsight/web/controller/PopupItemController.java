package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.popup.domain.port.in.dto.PopupItemCreateRequest;
import com.sleekydz86.finsight.core.popup.domain.port.in.dto.PopupItemResponse;
import com.sleekydz86.finsight.core.popup.domain.port.in.dto.PopupItemUpdateRequest;
import com.sleekydz86.finsight.core.popup.service.PopupItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "팝업 CMS", description = "팝업 항목 CRUD API")
@RestController
@RequestMapping("/api/v1/popup/items")
public class PopupItemController {

    private final PopupItemService popupItemService;

    public PopupItemController(PopupItemService popupItemService) {
        this.popupItemService = popupItemService;
    }

    @Operation(summary = "팝업 목록 조회", description = "팝업 항목 목록을 조회합니다. 스태프 인증이 적용될 수 있습니다.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<PopupItemResponse>>> list(
            @RequestParam(required = false) String domainId,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int p = Math.max(0, page);
        int s = Math.min(100, Math.max(1, size));
        return ResponseEntity.ok(
                ApiResponse.success(popupItemService.list(domainId, activeOnly, p, s), "팝업 목록을 조회했습니다"));
    }

    @Operation(summary = "팝업 상세 조회", description = "팝업 항목 상세를 조회합니다. 스태프 인증이 적용될 수 있습니다.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PopupItemResponse>> get(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(popupItemService.get(id), "팝업 상세를 조회했습니다"));
    }

    @Operation(summary = "팝업 등록", description = "팝업 항목을 등록합니다. 스태프 인증이 적용될 수 있습니다.")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping
    public ResponseEntity<ApiResponse<PopupItemResponse>> create(@Valid @RequestBody PopupItemCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(popupItemService.create(request), "팝업을 등록했습니다"));
    }

    @Operation(summary = "팝업 수정", description = "팝업 항목을 수정합니다. 스태프 인증이 적용될 수 있습니다.")
    @SecurityRequirement(name = "BearerAuth")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PopupItemResponse>> update(
            @PathVariable String id, @Valid @RequestBody PopupItemUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(popupItemService.update(id, request), "팝업을 수정했습니다"));
    }

    @Operation(summary = "팝업 삭제", description = "팝업 항목을 삭제합니다. 스태프 인증이 적용될 수 있습니다.")
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        popupItemService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "팝업을 삭제했습니다"));
    }
}
