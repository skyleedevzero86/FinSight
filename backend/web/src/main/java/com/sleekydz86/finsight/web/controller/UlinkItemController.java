package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.ulink.domain.port.in.dto.UlinkItemCreateRequest;
import com.sleekydz86.finsight.core.ulink.domain.port.in.dto.UlinkItemResponse;
import com.sleekydz86.finsight.core.ulink.domain.port.in.dto.UlinkItemUpdateRequest;
import com.sleekydz86.finsight.core.ulink.service.UlinkItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ulink/items")
public class UlinkItemController {

    private final UlinkItemService ulinkItemService;

    public UlinkItemController(UlinkItemService ulinkItemService) {
        this.ulinkItemService = ulinkItemService;
    }

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

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UlinkItemResponse>> get(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(ulinkItemService.get(id), "통합링크 상세를 조회했습니다"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UlinkItemResponse>> create(
            @Valid @RequestBody UlinkItemCreateRequest request) {
        UlinkItemResponse created = ulinkItemService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "통합링크 항목을 등록했습니다"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UlinkItemResponse>> update(
            @PathVariable String id, @Valid @RequestBody UlinkItemUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(ulinkItemService.update(id, request), "통합링크 항목을 수정했습니다"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        ulinkItemService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "통합링크 항목을 삭제했습니다"));
    }
}
