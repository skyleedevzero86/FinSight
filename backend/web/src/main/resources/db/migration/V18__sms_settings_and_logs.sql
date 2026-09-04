-- =============================================================================
-- V18: SMS 발송 설정·이력 (Solapi)
-- -----------------------------------------------------------------------------
-- 목적
--   - 관리자 SMS 마스터/용도별 자동 발송 on·off 설정 (sms_settings, 싱글톤 1행)
--   - Solapi SMS/LMS/MMS 발송·스킵·실패 이력 및 통계용 로그 (sms_send_log)
--   - OTP 등 민감 본문은 애플리케이션에서 content_preview 를 [REDACTED] 로 저장
--
-- sms_settings
--   - id=1 고정 싱글톤. 재실행 시 ON DUPLICATE KEY 로 기존 설정을 덮어쓰지 않음
--   - enabled=0 이면 용도별 스위치와 무관하게 자동 발송 스킵
--   - default_from_number 가 비어 있으면 설정값 solapi.default-from-number 사용
--
-- sms_send_log.status
--   - SENT    : Solapi(또는 시뮬레이션) 발송 성공
--   - FAILED  : 발송 실패 (error_message 에 사유)
--   - SKIPPED : 관리자 설정 비활성 등으로으로 의도적 미발송
--
-- sms_send_log.purpose
--   - NEWS_ALERT | OTP | ACCOUNT_RECOVERY | SYSTEM | NOTIFICATION | MANUAL
--
-- sms_send_log.message_type
--   - SMS | LMS | MMS | KAKAO_ALIMTALK | KAKAO_FRIENDTALK 등
--   - 관리자 수동 발송에서 카카오 타입은 API 단에서 거부 (UNSUPPORTED)
--
-- 연관 API
--   - GET/PUT  /api/v1/admin/sms/settings
--   - POST     /api/v1/admin/sms/send
--   - GET      /api/v1/admin/sms/balance
--   - POST     /api/v1/admin/sms/upload-image
--   - GET      /api/v1/admin/sms/logs   (status·purpose 단독/복합 필터)
--   - GET      /api/v1/admin/sms/stats
--   - (레거시) /api/notification/sms  → 관리자 권한 + ApiResponse
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1) SMS 발송 설정 (싱글톤)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sms_settings (
    -- 기본키 (항상 id=1 한 행)
    id BIGINT NOT NULL COMMENT '설정 PK (싱글톤 1행, 고정값 1)',

    -- 마스터·용도별 스위치 (0:꺼짐, 1:켜짐)
    enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'SMS 마스터 스위치 (0이면 전체 자동발송 스킵)',
    news_alert_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '뉴스 알림 자동 SMS 허용',
    otp_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'OTP SMS 발송 허용',
    account_recovery_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '계정 복구 SMS 발송 허용',
    system_alert_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '시스템 알림 SMS 발송 허용',
    notification_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '일반 알림 SMS 발송 허용',

    -- 발송 기본값
    default_message_type VARCHAR(16) NOT NULL DEFAULT 'SMS'
        COMMENT '기본 메시지 타입 (SMS|LMS|MMS)',
    default_from_number VARCHAR(32) NULL
        COMMENT '기본 발신번호 (NULL/빈값이면 solapi.default-from-number)',

    -- 감사 시각
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',

    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='SMS 발송 설정 (관리자 싱글톤)';

-- 초기 싱글톤 행 삽입.
-- created_at/updated_at 을 명시해, 기존 DB에 해당 컬럼 DEFAULT 가 없어도 안전하게 동작.
-- ON DUPLICATE KEY UPDATE id=id : 재마이그레이션·재실행 시 관리자가 바꾼 설정을 유지.
INSERT INTO sms_settings (
    id,
    enabled,
    news_alert_enabled,
    otp_enabled,
    account_recovery_enabled,
    system_alert_enabled,
    notification_enabled,
    default_message_type,
    created_at,
    updated_at
) VALUES (
    1,   -- 싱글톤 PK
    0,   -- 마스터 OFF (운영에서 키 확인 후 관리자 화면에서 ON)
    0,   -- 뉴스 알림 자동 SMS OFF
    1,   -- OTP ON
    1,   -- 계정 복구 ON
    0,   -- 시스템 알림 OFF
    0,   -- 일반 알림 OFF
    'SMS',
    CURRENT_TIMESTAMP(3),
    CURRENT_TIMESTAMP(3)
) ON DUPLICATE KEY UPDATE id = id;

-- -----------------------------------------------------------------------------
-- 2) SMS 발송 이력 (통계·관리자 로그 조회)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sms_send_log (
    -- 기본키
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'SMS 발송 이력 PK',

    -- 발송 분류
    purpose VARCHAR(32) NOT NULL
        COMMENT '용도 (NEWS_ALERT|OTP|ACCOUNT_RECOVERY|SYSTEM|NOTIFICATION|MANUAL)',
    message_type VARCHAR(16) NOT NULL
        COMMENT '메시지 타입 (SMS|LMS|MMS|KAKAO_ALIMTALK|KAKAO_FRIENDTALK 등)',

    -- 수신·발신
    to_phone VARCHAR(32) NOT NULL COMMENT '수신 전화번호',
    from_phone VARCHAR(32) NULL COMMENT '발신 전화번호',

    -- 본문·결과 (OTP purpose 는 앱에서 [REDACTED] 저장·응답)
    content_preview VARCHAR(500) NULL COMMENT '본문 미리보기 (OTP는 [REDACTED])',
    status VARCHAR(16) NOT NULL COMMENT '발송 결과 (SENT|FAILED|SKIPPED)',
    external_message_id VARCHAR(100) NULL COMMENT 'Solapi(외부) 메시지 ID',
    error_message VARCHAR(1000) NULL COMMENT '실패 또는 스킵 사유',

    -- 연관 사용자
    recipient_user_id BIGINT NULL COMMENT '수신 사용자 ID (회원 매핑 시)',
    actor_user_id BIGINT NULL COMMENT '발송 요청자(관리자) 사용자 ID (수동 발송 시)',

    -- 시각
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '발송(기록) 시각',

    PRIMARY KEY (id),
    -- 관리자 로그: 최신순·상태·용도·수신번호 필터
    KEY idx_sms_log_created (created_at),
    KEY idx_sms_log_status (status),
    KEY idx_sms_log_purpose (purpose),
    KEY idx_sms_log_to_phone (to_phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='SMS 발송 이력 (성공·실패·스킵)';
