-- =============================================================================
-- V18: SMS 발송 설정·이력
-- -----------------------------------------------------------------------------
-- 목적
--   - 관리자 SMS 발송 on/off·자동발송 채널 설정(sms_settings)
--   - Solapi SMS/LMS/MMS 발송 결과 이력·통계(sms_send_log)
--
-- 연관 API
--   - GET/PUT  /api/v1/admin/sms/settings
--   - POST     /api/v1/admin/sms/send
--   - GET      /api/v1/admin/sms/balance
--   - POST     /api/v1/admin/sms/upload-image
--   - GET      /api/v1/admin/sms/logs
--   - GET      /api/v1/admin/sms/stats
-- =============================================================================

CREATE TABLE IF NOT EXISTS sms_settings (
    id BIGINT NOT NULL COMMENT '설정 PK (싱글톤 1행)',
    enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'SMS 마스터 스위치',
    news_alert_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '뉴스 알림 자동 SMS',
    otp_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'OTP SMS 발송',
    account_recovery_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '계정복구 SMS',
    system_alert_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '시스템 알림 SMS',
    notification_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '일반 알림 SMS',
    default_message_type VARCHAR(16) NOT NULL DEFAULT 'SMS' COMMENT '기본 메시지 타입 (SMS|LMS|MMS)',
    default_from_number VARCHAR(32) NULL COMMENT '기본 발신번호 (비어있으면 solapi.default-from-number)',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='SMS 발송 설정 (관리자)';

INSERT INTO sms_settings (
    id, enabled, news_alert_enabled, otp_enabled, account_recovery_enabled,
    system_alert_enabled, notification_enabled, default_message_type
) VALUES (
    1, 0, 0, 1, 1, 0, 0, 'SMS'
) ON DUPLICATE KEY UPDATE id = id;

CREATE TABLE IF NOT EXISTS sms_send_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'SMS 발송 이력 PK',
    purpose VARCHAR(32) NOT NULL COMMENT '용도 (NEWS_ALERT|OTP|ACCOUNT_RECOVERY|SYSTEM|NOTIFICATION|MANUAL)',
    message_type VARCHAR(16) NOT NULL COMMENT '메시지 타입 (SMS|LMS|MMS|...)',
    to_phone VARCHAR(32) NOT NULL COMMENT '수신 번호',
    from_phone VARCHAR(32) NULL COMMENT '발신 번호',
    content_preview VARCHAR(500) NULL COMMENT '본문 미리보기',
    status VARCHAR(16) NOT NULL COMMENT 'SENT|FAILED|SKIPPED',
    external_message_id VARCHAR(100) NULL COMMENT 'Solapi 메시지 ID',
    error_message VARCHAR(1000) NULL COMMENT '실패/스킵 사유',
    recipient_user_id BIGINT NULL COMMENT '수신 사용자 ID',
    actor_user_id BIGINT NULL COMMENT '발송 요청자(관리자) ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '발송(기록) 시각',

    PRIMARY KEY (id),
    KEY idx_sms_log_created (created_at),
    KEY idx_sms_log_status (status),
    KEY idx_sms_log_purpose (purpose),
    KEY idx_sms_log_to_phone (to_phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='SMS 발송 이력';
