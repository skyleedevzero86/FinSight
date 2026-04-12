package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.mainimg.domain.port.in.dto.MainimgItemCreateRequest;
import com.sleekydz86.finsight.core.mainimg.domain.port.in.dto.MainimgItemResponse;
import com.sleekydz86.finsight.core.mainimg.domain.port.in.dto.MainimgItemUpdateRequest;
import com.sleekydz86.finsight.core.mainimg.service.MainimgItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mainimg/items")
public class MainimgItemController {

    private final MainimgItemService mainimgItemService;

    public MainimgItemController(MainimgItemService mainimgItemService) {
        this.mainimgItemService = mainimgItemService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<MainimgItemResponse>>> list(
            @RequestParam(required = false) String domainId,
            @RequestParam(defaultValue = "true") boolean reflectOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int p = Math.max(0, page);
        int s = Math.min(100, Math.max(1, size));
        return ResponseEntity.ok(
                ApiResponse.success(mainimgItemService.list(domainId, reflectOnly, p, s), "메인이미지 목록을 조회했습니다"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MainimgItemResponse>> get(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(mainimgItemService.get(id), "메인이미지 상세를 조회했습니다"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MainimgItemResponse>> create(@Valid @RequestBody MainimgItemCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(mainimgItemService.create(request), "메인이미지를 등록했습니다"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MainimgItemResponse>> update(
            @PathVariable String id, @Valid @RequestBody MainimgItemUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(mainimgItemService.update(id, request), "메인이미지를 수정했습니다"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        mainimgItemService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "메인이미지를 삭제했습니다"));
    }
}
