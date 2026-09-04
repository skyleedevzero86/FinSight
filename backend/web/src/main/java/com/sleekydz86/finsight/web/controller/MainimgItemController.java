package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.mainimg.domain.port.in.dto.MainimgItemCreateRequest;
import com.sleekydz86.finsight.core.mainimg.domain.port.in.dto.MainimgItemResponse;
import com.sleekydz86.finsight.core.mainimg.domain.port.in.dto.MainimgItemUpdateRequest;
import com.sleekydz86.finsight.core.mainimg.service.MainimgItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "메인이미지 CMS", description = "메인이미지 항목 CRUD API")
@RestController
@RequestMapping("/api/v1/mainimg/items")
public class MainimgItemController {

    private final MainimgItemService mainimgItemService;

    public MainimgItemController(MainimgItemService mainimgItemService) {
        this.mainimgItemService = mainimgItemService;
    }

    @Operation(summary = "메인이미지 목록 조회", description = "메인이미지 항목 목록을 조회합니다. 스태프 인증이 적용될 수 있습니다.")
    @SecurityRequirement(name = "BearerAuth")
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

    @Operation(summary = "메인이미지 상세 조회", description = "메인이미지 항목 상세를 조회합니다. 스태프 인증이 적용될 수 있습니다.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MainimgItemResponse>> get(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(mainimgItemService.get(id), "메인이미지 상세를 조회했습니다"));
    }

    @Operation(summary = "메인이미지 등록", description = "메인이미지 항목을 등록합니다. 스태프 인증이 적용될 수 있습니다.")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping
    public ResponseEntity<ApiResponse<MainimgItemResponse>> create(@Valid @RequestBody MainimgItemCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(mainimgItemService.create(request), "메인이미지를 등록했습니다"));
    }

    @Operation(summary = "메인이미지 수정", description = "메인이미지 항목을 수정합니다. 스태프 인증이 적용될 수 있습니다.")
    @SecurityRequirement(name = "BearerAuth")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MainimgItemResponse>> update(
            @PathVariable String id, @Valid @RequestBody MainimgItemUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(mainimgItemService.update(id, request), "메인이미지를 수정했습니다"));
    }

    @Operation(summary = "메인이미지 삭제", description = "메인이미지 항목을 삭제합니다. 스태프 인증이 적용될 수 있습니다.")
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        mainimgItemService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "메인이미지를 삭제했습니다"));
    }
}
