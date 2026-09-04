package com.sleekydz86.finsight.core.notification.domain.port.in.dto;

import com.sleekydz86.finsight.core.notification.domain.dto.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "관리자 SMS 수동 발송 요청")
public record SmsManualSendRequest(
        @Schema(description = "수신 전화번호 (userEmail과 둘 중 하나 필수)", example = "01012345678")
        @Size(max = 32)
        String toPhone,
        @Schema(description = "수신 사용자 이메일 (toPhone과 둘 중 하나 필수)")
        @Size(max = 255)
        String userEmail,
        @NotBlank @Size(max = 2000)
        @Schema(description = "메시지 본문", requiredMode = Schema.RequiredMode.REQUIRED)
        String message,
        @Schema(description = "메시지 타입", example = "SMS")
        MessageType messageType,
        @Schema(description = "LMS/MMS 제목")
        @Size(max = 40)
        String subject,
        @Schema(description = "MMS 이미지 ID (upload-image 결과)")
        @Size(max = 100)
        String imageId
) {
}
