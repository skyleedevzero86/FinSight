package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.notification.service.SmsNotificationService;
import com.sleekydz86.finsight.core.notification.service.SolapiMessageService;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/notification/sms")
@RequiredArgsConstructor
public class SmsNotificationController {

    private final SmsNotificationService smsNotificationService;
    private final SolapiMessageService solapiMessageService;
    private final UserService userService;

    @PostMapping("/send")
    public ResponseEntity<String> sendSms(
            @RequestParam String userEmail,
            @RequestParam String message) {

        try {
            User user = userService.findByEmail(userEmail);
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