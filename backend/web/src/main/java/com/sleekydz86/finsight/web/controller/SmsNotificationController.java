package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.notification.service.SmsNotificationService;
import com.sleekydz86.finsight.core.notification.service.SolapiMessageService;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Optional;

@Slf4j
@Tag(name = "SMS 알림", description = "Solapi SMS 발송·잔액·이미지 업로드 API")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/notification/sms")
@RequiredArgsConstructor
public class SmsNotificationController {

    private final SmsNotificationService smsNotificationService;
    private final SolapiMessageService solapiMessageService;
    private final UserService userService;

    @Operation(summary = "SMS 발송", description = "지정 사용자에게 Solapi SMS를 발송합니다.")
    @PostMapping("/send")
    public ResponseEntity<String> sendSms(
            @RequestParam String userEmail,
            @RequestParam String message) {

        try {
            Optional<User> userOptional = userService.findByEmail(userEmail);
            if (userOptional.isEmpty()) {
                return ResponseEntity.badRequest().body("사용자를 찾을 수 없습니다: " + userEmail);
            }

            User user = userOptional.get();
            smsNotificationService.sendNotification(user,
                    com.sleekydz86.finsight.core.notification.domain.Notification.builder()
                            .title("FinSight 알림")
                            .content(message)
                            .build());

            return ResponseEntity.ok("SMS 발송 완료");
        } catch (Exception e) {
            log.error("SMS 발송 실패", e);
            return ResponseEntity.badRequest().body("SMS 발송 실패: " + e.getMessage());
        }
    }

    @Operation(summary = "SMS 이미지 업로드", description = "Solapi MMS용 이미지를 업로드합니다.")
    @PostMapping("/upload-image")
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String imageId = solapiMessageService.uploadImage(file);
            return ResponseEntity.ok(imageId);
        } catch (IOException e) {
            log.error("이미지 업로드 실패", e);
            return ResponseEntity.badRequest().body("이미지 업로드 실패: " + e.getMessage());
        }
    }

    @Operation(summary = "SMS 잔액 조회", description = "Solapi 계정 잔액을 조회합니다.")
    @GetMapping("/balance")
    public ResponseEntity<String> getBalance() {
        try {
            String balance = solapiMessageService.getBalance();
            return ResponseEntity.ok(balance);
        } catch (Exception e) {
            log.error("잔액 조회 실패", e);
            return ResponseEntity.badRequest().body("잔액 조회 실패: " + e.getMessage());
        }
    }
}
