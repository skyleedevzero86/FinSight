package com.sleekydz86.finsight.core.notification.service;

import com.sleekydz86.finsight.core.notification.domain.dto.MessageSendResult;
import com.sleekydz86.finsight.core.notification.domain.dto.MessageType;
import com.sleekydz86.finsight.core.notification.domain.dto.SolapiMessageRequest;
import com.sleekydz86.finsight.core.notification.domain.dto.SolapiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SolapiMessageService {

    private final SolapiProperties solapiProperties;

    public MessageSendResult sendSms(String to, String text) {
        return sendSms(to, text, null);
    }

    public MessageSendResult sendSms(String to, String text, String from) {
        SolapiMessageRequest request = SolapiMessageRequest.builder()
                .from(from != null ? from : solapiProperties.getDefaultFromNumber())
                .to(to)
                .text(text)
                .messageType(MessageType.SMS)
                .build();

        return sendMessage(request);
    }

    public MessageSendResult sendLms(String to, String text, String subject) {
        return sendLms(to, text, subject, null);
    }

    public MessageSendResult sendLms(String to, String text, String subject, String from) {
        SolapiMessageRequest request = SolapiMessageRequest.builder()
                .from(from != null ? from : solapiProperties.getDefaultFromNumber())
                .to(to)
                .text(text)
                .subject(subject)
                .messageType(MessageType.LMS)
                .build();

        return sendMessage(request);
    }

    public MessageSendResult sendMms(String to, String text, String subject, String imageId) {
        return sendMms(to, text, subject, imageId, null);
    }

    public MessageSendResult sendMms(String to, String text, String subject, String imageId, String from) {
        SolapiMessageRequest request = SolapiMessageRequest.builder()
                .from(from != null ? from : solapiProperties.getDefaultFromNumber())
                .to(to)
                .text(text)
                .subject(subject)
                .imageId(imageId)
                .messageType(MessageType.MMS)
                .build();

        return sendMessage(request);
    }

    public MessageSendResult sendKakaoAlimtalk(String to, String text, String templateId, String pfId) {
        SolapiMessageRequest.KakaoMessageOptions kakaoOptions =
                SolapiMessageRequest.KakaoMessageOptions.builder()
                        .templateId(templateId)
                        .pfId(pfId)
                        .build();

        SolapiMessageRequest request = SolapiMessageRequest.builder()
                .from(solapiProperties.getDefaultFromNumber())
                .to(to)
                .text(text)
                .messageType(MessageType.KAKAO_ALIMTALK)
                .kakaoOptions(kakaoOptions)
                .build();

        return sendMessage(request);
    }

    public MessageSendResult sendScheduledMessage(String to, String text, LocalDateTime scheduledDate) {
        SolapiMessageRequest request = SolapiMessageRequest.builder()
                .from(solapiProperties.getDefaultFromNumber())
                .to(to)
                .text(text)
                .messageType(MessageType.SMS)
                .scheduledDate(scheduledDate)
                .build();

        return sendMessage(request);
    }

    public MessageSendResult sendMessage(SolapiMessageRequest request) {
        try {
            log.info("=== SOLAPI 메시지 발송 시작 ===");
            log.info("발신번호: {}", request.getFrom());
            log.info("수신번호: {}", request.getTo());
            log.info("메시지 타입: {}", request.getMessageType());
            log.info("내용: {}", request.getText());

            if (request.getSubject() != null) {
                log.info("제목: {}", request.getSubject());
            }
            if (request.getScheduledDate() != null) {
                log.info("예약 시간: {}", request.getScheduledDate());
            }

            String messageId = "SOLAPI_" + System.currentTimeMillis();

            log.info("SOLAPI 메시지 발송 성공! 메시지 ID: {}", messageId);

            return MessageSendResult.success(
                    messageId,
                    request.getMessageType(),
                    request.getFrom(),
                    request.getTo()
            );

        } catch (Exception e) {
            log.error("SOLAPI 메시지 발송 중 오류: {}", e.getMessage(), e);
            return MessageSendResult.failure(e.getMessage(), "SOLAPI_ERROR");
        }
    }

    public String uploadImage(MultipartFile file) throws IOException {
        try {
            log.info("=== SOLAPI 이미지 업로드 시작 ===");
            log.info("파일명: {}", file.getOriginalFilename());
            log.info("파일 크기: {} bytes", file.getSize());

            String imageId = "SIM_IMG_" + System.currentTimeMillis();

            log.info("이미지 업로드 성공: {}", imageId);
            return imageId;

        } catch (Exception e) {
            log.error("이미지 업로드 실패: {}", e.getMessage(), e);
            throw new IOException("이미지 업로드 실패: " + e.getMessage(), e);
        }
    }

    public String uploadKakaoImage(MultipartFile file, String link) throws IOException {
        try {
            log.info("=== SOLAPI 카카오 이미지 업로드 시작 ===");
            log.info("파일명: {}", file.getOriginalFilename());
            log.info("링크: {}", link);

            String imageId = "SIM_KAKAO_IMG_" + System.currentTimeMillis();

            log.info("카카오 이미지 업로드 성공: {}", imageId);
            return imageId;

        } catch (Exception e) {
            log.error("카카오 이미지 업로드 실패: {}", e.getMessage(), e);
            throw new IOException("카카오 이미지 업로드 실패: " + e.getMessage(), e);
        }
    }

    public String getBalance() {
        try {
            log.info("=== SOLAPI 잔액 조회 시작 ===");

            String balance = "시뮬레이션 모드 - 잔액 조회 기능은 SOLAPI SDK 설정 후 사용 가능합니다.";

            log.info("시뮬레이션 잔액 조회: {}", balance);
            return balance;

        } catch (Exception e) {
            log.error("잔액 조회 실패: {}", e.getMessage(), e);
            return "잔액 조회 실패: " + e.getMessage();
        }
    }

    public boolean supports(String messageType) {
        try {
            MessageType.valueOf(messageType);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}