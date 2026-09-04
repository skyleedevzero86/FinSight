package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.notification.domain.port.in.dto.SmsBalanceResponse;
import com.sleekydz86.finsight.core.notification.domain.port.in.dto.SmsManualSendRequest;
import com.sleekydz86.finsight.core.notification.domain.port.in.dto.SmsManualSendResponse;
import com.sleekydz86.finsight.core.notification.service.SmsAdminService;
import com.sleekydz86.finsight.core.global.annotation.CurrentUser;
import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Tag(
        name = "SMS 알림",
        description = "Solapi SMS 발송·잔액·이미지 업로드(레거시 경로). 관리 UI는 /api/v1/admin/sms 권장.")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/notification/sms")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class SmsNotificationController {

    private final SmsAdminService smsAdminService;

    @Operation(summary = "SMS 발송", description = "레거시. userEmail+message. 신규는 POST /api/v1/admin/sms/send")
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<SmsManualSendResponse>> sendSms(
            @CurrentUser AuthenticatedUser currentUser,
            @RequestParam String userEmail,
            @RequestParam String message) {
        SmsManualSendResponse result = smsAdminService.sendManual(
                new SmsManualSendRequest(null, userEmail, message, null, null, null),
                currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(result, result.success() ? "SMS 발송 완료" : "SMS 발송 실패"));
    }

    @Operation(summary = "SMS 이미지 업로드", description = "Solapi MMS용 이미지 업로드")
    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(@RequestParam("file") MultipartFile file)
            throws IOException {
        String imageId = smsAdminService.uploadImage(file);
        return ResponseEntity.ok(ApiResponse.success(Map.of("imageId", imageId), "이미지 업로드 완료"));
    }

    @Operation(summary = "SMS 잔액 조회", description = "Solapi 계정 잔액")
    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<SmsBalanceResponse>> getBalance() {
        return ResponseEntity.ok(ApiResponse.success(smsAdminService.balance(), "잔액 조회 완료"));
    }
}
