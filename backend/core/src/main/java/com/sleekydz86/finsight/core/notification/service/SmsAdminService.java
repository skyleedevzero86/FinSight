package com.sleekydz86.finsight.core.notification.service;

import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.notification.adapter.persistence.SmsSendLogJpaEntity;
import com.sleekydz86.finsight.core.notification.adapter.persistence.SmsSendLogJpaRepository;
import com.sleekydz86.finsight.core.notification.adapter.persistence.SmsSettingsJpaEntity;
import com.sleekydz86.finsight.core.notification.adapter.persistence.SmsSettingsJpaRepository;
import com.sleekydz86.finsight.core.notification.domain.SmsPurpose;
import com.sleekydz86.finsight.core.notification.domain.SmsSendStatus;
import com.sleekydz86.finsight.core.notification.domain.dto.MessageSendResult;
import com.sleekydz86.finsight.core.notification.domain.dto.MessageType;
import com.sleekydz86.finsight.core.notification.domain.dto.SolapiProperties;
import com.sleekydz86.finsight.core.notification.domain.port.in.dto.*;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsAdminService {

    private final SmsSettingsJpaRepository smsSettingsJpaRepository;
    private final SmsSendLogJpaRepository smsSendLogJpaRepository;
    private final SolapiMessageService solapiMessageService;
    private final SolapiProperties solapiProperties;
    private final UserPersistencePort userPersistencePort;

    @Transactional
    public SmsSettingsResponse getSettings() {
        return toSettingsResponse(getOrCreateSettings());
    }

    @Transactional
    public SmsSettingsResponse updateSettings(SmsSettingsUpdateRequest request) {
        SmsSettingsJpaEntity settings = getOrCreateSettings();
        settings.apply(
                Boolean.TRUE.equals(request.enabled()),
                Boolean.TRUE.equals(request.newsAlertEnabled()),
                Boolean.TRUE.equals(request.otpEnabled()),
                Boolean.TRUE.equals(request.accountRecoveryEnabled()),
                Boolean.TRUE.equals(request.systemAlertEnabled()),
                Boolean.TRUE.equals(request.notificationEnabled()),
                request.defaultMessageType(),
                request.defaultFromNumber());
        smsSettingsJpaRepository.save(settings);
        log.info("SMS 발송 설정 저장 - enabled={}, news={}, otp={}, recovery={}, system={}, notification={}",
                settings.isEnabled(),
                settings.isNewsAlertEnabled(),
                settings.isOtpEnabled(),
                settings.isAccountRecoveryEnabled(),
                settings.isSystemAlertEnabled(),
                settings.isNotificationEnabled());
        return toSettingsResponse(settings);
    }

    private SmsSettingsJpaEntity getOrCreateSettings() {
        return smsSettingsJpaRepository.findById(1L).orElseGet(() -> {
            SmsSettingsJpaEntity created = SmsSettingsJpaEntity.defaults();
            return smsSettingsJpaRepository.save(created);
        });
    }

    @Transactional(readOnly = true)
    public SmsSettingsJpaEntity peekSettings() {
        return smsSettingsJpaRepository.findById(1L).orElseGet(SmsSettingsJpaEntity::defaults);
    }

    @Transactional(readOnly = true)
    public boolean isPurposeEnabled(SmsPurpose purpose) {
        SmsSettingsJpaEntity settings = peekSettings();
        if (!settings.isEnabled()) {
            return false;
        }
        return switch (purpose) {
            case NEWS_ALERT -> settings.isNewsAlertEnabled();
            case OTP -> settings.isOtpEnabled();
            case ACCOUNT_RECOVERY -> settings.isAccountRecoveryEnabled();
            case SYSTEM -> settings.isSystemAlertEnabled();
            case NOTIFICATION -> settings.isNotificationEnabled();
            case MANUAL -> true;
        };
    }

    @Transactional(readOnly = true)
    public String resolveFromNumber() {
        SmsSettingsJpaEntity settings = peekSettings();
        if (settings.getDefaultFromNumber() != null && !settings.getDefaultFromNumber().isBlank()) {
            return settings.getDefaultFromNumber();
        }
        return solapiProperties.getDefaultFromNumber();
    }

    @Transactional
    public SmsManualSendResponse sendManual(SmsManualSendRequest request, Long actorUserId) {
        if (!isPurposeEnabled(SmsPurpose.MANUAL)) {
            return new SmsManualSendResponse(false, SmsSendStatus.SKIPPED, MessageType.SMS, null, null,
                    "SMS 마스터 스위치가 꺼져 있습니다.");
        }

        String phone = request.toPhone();
        Long recipientUserId = null;
        if ((phone == null || phone.isBlank()) && request.userEmail() != null && !request.userEmail().isBlank()) {
            User user = userPersistencePort.findByEmail(request.userEmail().trim())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            phone = user.getPhoneNumber();
            recipientUserId = user.getId();
            if (phone == null || phone.isBlank()) {
                throw new IllegalArgumentException("해당 사용자에게 등록된 전화번호가 없습니다.");
            }
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("수신 전화번호 또는 사용자 이메일이 필요합니다.");
        }

        MessageType type = request.messageType() != null ? request.messageType() : MessageType.SMS;
        String from = resolveFromNumber();
        MessageSendResult result = switch (type) {
            case LMS -> solapiMessageService.sendLms(phone, request.message(),
                    request.subject() != null ? request.subject() : "FinSight", from);
            case MMS -> solapiMessageService.sendMms(phone, request.message(),
                    request.subject() != null ? request.subject() : "FinSight",
                    request.imageId(), from);
            default -> solapiMessageService.sendSms(phone, request.message(), from);
        };

        SmsSendStatus status = result.isSuccess() ? SmsSendStatus.SENT : SmsSendStatus.FAILED;
        saveLog(SmsPurpose.MANUAL, type, phone, from, request.message(), status,
                result.getMessageId(), result.getErrorMessage(), recipientUserId, actorUserId);

        return new SmsManualSendResponse(
                result.isSuccess(),
                status,
                type,
                result.getMessageId(),
                phone,
                result.getErrorMessage());
    }

    @Transactional
    public void recordSend(
            SmsPurpose purpose,
            MessageType messageType,
            String toPhone,
            String fromPhone,
            String content,
            MessageSendResult result,
            Long recipientUserId,
            Long actorUserId) {
        SmsSendStatus status = result != null && result.isSuccess() ? SmsSendStatus.SENT : SmsSendStatus.FAILED;
        saveLog(purpose, messageType, toPhone, fromPhone, content, status,
                result != null ? result.getMessageId() : null,
                result != null ? result.getErrorMessage() : "결과 없음",
                recipientUserId, actorUserId);
    }

    @Transactional
    public void recordSkipped(SmsPurpose purpose, String toPhone, String reason, Long recipientUserId) {
        saveLog(purpose, MessageType.SMS, toPhone != null ? toPhone : "-", null, null,
                SmsSendStatus.SKIPPED, null, reason, recipientUserId, null);
    }

    @Transactional(readOnly = true)
    public PaginationResponse<SmsSendLogResponse> listLogs(SmsSendStatus status, SmsPurpose purpose, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)));
        Page<SmsSendLogJpaEntity> result;
        if (status != null) {
            result = smsSendLogJpaRepository.findByStatusOrderByCreatedAtDescIdDesc(status, pageable);
        } else if (purpose != null) {
            result = smsSendLogJpaRepository.findByPurposeOrderByCreatedAtDescIdDesc(purpose, pageable);
        } else {
            result = smsSendLogJpaRepository.findAllByOrderByCreatedAtDescIdDesc(pageable);
        }
        return PaginationResponse.from(result.map(this::toLogResponse));
    }

    @Transactional(readOnly = true)
    public SmsStatsResponse stats() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        long totalSent = smsSendLogJpaRepository.countByStatus(SmsSendStatus.SENT);
        long totalFailed = smsSendLogJpaRepository.countByStatus(SmsSendStatus.FAILED);
        long totalSkipped = smsSendLogJpaRepository.countByStatus(SmsSendStatus.SKIPPED);
        long sentLast7 = smsSendLogJpaRepository.countByStatusAndCreatedAtAfter(SmsSendStatus.SENT, weekAgo);
        long failedLast7 = smsSendLogJpaRepository.countByStatusAndCreatedAtAfter(SmsSendStatus.FAILED, weekAgo);

        Map<String, Long> byPurpose = new LinkedHashMap<>();
        for (SmsPurpose purpose : SmsPurpose.values()) {
            byPurpose.put(purpose.name(), smsSendLogJpaRepository.countByPurpose(purpose));
        }

        Map<LocalDate, long[]> dailyMap = new LinkedHashMap<>();
        for (Object[] row : smsSendLogJpaRepository.countDailyStatusSince(weekAgo)) {
            LocalDate date = toLocalDate(row[0]);
            SmsSendStatus st = (SmsSendStatus) row[1];
            long count = row[2] instanceof Number n ? n.longValue() : 0L;
            long[] bucket = dailyMap.computeIfAbsent(date, d -> new long[3]);
            switch (st) {
                case SENT -> bucket[0] = count;
                case FAILED -> bucket[1] = count;
                case SKIPPED -> bucket[2] = count;
            }
        }
        List<SmsStatsResponse.DailyPoint> daily = new ArrayList<>();
        for (Map.Entry<LocalDate, long[]> e : dailyMap.entrySet()) {
            long[] b = e.getValue();
            daily.add(new SmsStatsResponse.DailyPoint(e.getKey().toString(), b[0], b[1], b[2]));
        }

        return new SmsStatsResponse(totalSent, totalFailed, totalSkipped, sentLast7, failedLast7, byPurpose, daily);
    }

    @Transactional(readOnly = true)
    public SmsBalanceResponse balance() {
        String text = solapiMessageService.getBalance();
        boolean simulation = !solapiProperties.isEnabled();
        return new SmsBalanceResponse(text, simulation);
    }

    public String uploadImage(MultipartFile file) throws IOException {
        return solapiMessageService.uploadImage(file);
    }

    private void saveLog(
            SmsPurpose purpose,
            MessageType messageType,
            String toPhone,
            String fromPhone,
            String content,
            SmsSendStatus status,
            String externalId,
            String errorMessage,
            Long recipientUserId,
            Long actorUserId) {
        String preview = content == null ? null
                : (content.length() > 480 ? content.substring(0, 480) + "…" : content);
        smsSendLogJpaRepository.save(SmsSendLogJpaEntity.builder()
                .purpose(purpose)
                .messageType(messageType != null ? messageType : MessageType.SMS)
                .toPhone(toPhone)
                .fromPhone(fromPhone)
                .contentPreview(preview)
                .status(status)
                .externalMessageId(externalId)
                .errorMessage(errorMessage)
                .recipientUserId(recipientUserId)
                .actorUserId(actorUserId)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private SmsSettingsResponse toSettingsResponse(SmsSettingsJpaEntity s) {
        return new SmsSettingsResponse(
                s.isEnabled(),
                s.isNewsAlertEnabled(),
                s.isOtpEnabled(),
                s.isAccountRecoveryEnabled(),
                s.isSystemAlertEnabled(),
                s.isNotificationEnabled(),
                s.getDefaultMessageType(),
                s.getDefaultFromNumber(),
                solapiProperties.isEnabled());
    }

    private SmsSendLogResponse toLogResponse(SmsSendLogJpaEntity e) {
        return new SmsSendLogResponse(
                e.getId(),
                e.getPurpose(),
                e.getPurpose().getLabel(),
                e.getMessageType(),
                e.getToPhone(),
                e.getFromPhone(),
                e.getContentPreview(),
                e.getStatus(),
                e.getExternalMessageId(),
                e.getErrorMessage(),
                e.getRecipientUserId(),
                e.getActorUserId(),
                e.getCreatedAt());
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }
}
