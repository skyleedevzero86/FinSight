package com.sleekydz86.finsight.web.controller.admin;

import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.exception.SystemException;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareDetection;
import com.sleekydz86.finsight.core.portfolio.service.PortfolioShareDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/portfolio")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AdminPortfolioController {

    private static final Logger log = LoggerFactory.getLogger(AdminPortfolioController.class);

    private final PortfolioShareDetectionService detectionService;

    public AdminPortfolioController(PortfolioShareDetectionService detectionService) {
        this.detectionService = detectionService;
    }

    @GetMapping("/detections")
    public ResponseEntity<ApiResponse<PortfolioShareDetection>> detections() {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    detectionService.detect(),
                    "공유 자동 탐지를 조회했습니다"));
        } catch (RuntimeException exception) {
            log.error("공유 자동 탐지 실패", exception);
            throw new SystemException("공유 자동 탐지를 불러오지 못했습니다.", "PORTFOLIO_SHARE_DETECTION_ERROR", exception);
        }
    }
}
