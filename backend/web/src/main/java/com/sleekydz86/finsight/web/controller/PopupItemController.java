package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.popup.domain.port.in.dto.PopupItemCreateRequest;
import com.sleekydz86.finsight.core.popup.domain.port.in.dto.PopupItemResponse;
import com.sleekydz86.finsight.core.popup.domain.port.in.dto.PopupItemUpdateRequest;
import com.sleekydz86.finsight.core.popup.service.PopupItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/popup/items")
public class PopupItemController {

    private final PopupItemService popupItemService;

    public PopupItemController(PopupItemService popupItemService) {
        this.popupItemService = popupItemService;
    }

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

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PopupItemResponse>> get(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(popupItemService.get(id), "팝업 상세를 조회했습니다"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PopupItemResponse>> create(@Valid @RequestBody PopupItemCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(popupItemService.create(request), "팝업을 등록했습니다"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PopupItemResponse>> update(
            @PathVariable String id, @Valid @RequestBody PopupItemUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(popupItemService.update(id, request), "팝업을 수정했습니다"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        popupItemService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "팝업을 삭제했습니다"));
    }
}
