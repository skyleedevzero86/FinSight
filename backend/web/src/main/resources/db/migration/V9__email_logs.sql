-- =============================================================================
-- V9: 메일 발송 이력 (email_logs)
-- -----------------------------------------------------------------------------
-- 목적
--   - 시스템/사용자/비로그인 요청으로 나간 모든 메일의 발송 결과를 보관한다.
--   - 관리자가 언제·누구에게·어떤 용도로·어디서(IP) 요청했는지 추적할 수 있게 한다.
--
-- 비로그인 정책
--   - actor_type = 'ANONYMOUS' 인 경우 user_id 는 NULL 일 수 있다.
--   - 대신 request_ip / request_location / user_agent 로 요청자를 식별·관리한다.
--
-- 마이그레이션 주의
--   - 이전 EmailLog 스텁이 ddl-auto 로 부분 생성됐을 수 있어 테이블을 재생성한다.
--   - 당시에는 실제 발송 이력으로 쓰이지 않았으므로 DROP 후 CREATE 한다.
-- =============================================================================

DROP TABLE IF EXISTS email_logs;

CREATE TABLE email_logs (
    -- 기본키
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '메일 발송 이력 PK',

    -- 메일 헤더/본문 요약
    recipient VARCHAR(255) NOT NULL COMMENT '수신 이메일 주소',
    subject VARCHAR(500) NOT NULL COMMENT '메일 제목',
    template_type VARCHAR(64) NULL COMMENT '템플릿/발송 유형 코드 (예: verification-code)',
    purpose VARCHAR(64) NOT NULL COMMENT '발송 용도 enum (VERIFICATION_SIGNUP, NEWS_ALERT 등)',
    purpose_label VARCHAR(100) NOT NULL COMMENT '발송 용도 한글 라벨',
    status VARCHAR(20) NOT NULL COMMENT '발송 상태 (SENT, FAILED 등)',
    from_address VARCHAR(255) NULL COMMENT '발신 이메일 주소(SMTP username)',

    -- 사용자/요청 주체
    user_id BIGINT NULL COMMENT '관련 사용자 ID (비로그인이면 NULL 가능)',
    actor_type VARCHAR(20) NOT NULL COMMENT '요청 주체 (ANONYMOUS, USER, SYSTEM, ADMIN)',
    actor_user_id BIGINT NULL COMMENT '실제 조작한 사용자 ID(관리자/본인 등)',

    -- 비로그인·보안 추적
    request_ip VARCHAR(64) NULL COMMENT '요청 클라이언트 IP (비로그인 추적용)',
    request_location VARCHAR(255) NULL COMMENT 'IP 기반 대략 위치 문자열',
    user_agent VARCHAR(512) NULL COMMENT '요청 User-Agent',

    -- 본문/오류 (민감정보 마스킹된 미리보기만 저장)
    body_preview TEXT NULL COMMENT '본문 미리보기(HTML 제거, OTP/인증코드 마스킹)',
    error_message TEXT NULL COMMENT '발송 실패 시 오류 메시지',
    related_ref VARCHAR(100) NULL COMMENT '연관 참조키 (예: email verification challengeId)',

    -- 전달/열람 확장 필드 (현재는 SMTP 단방향이라 대부분 NULL)
    sent_at DATETIME(6) NULL COMMENT 'SMTP 전송 성공 시각',
    delivered_at DATETIME(6) NULL COMMENT '수신 서버 전달 시각(확장용)',
    opened_at DATETIME(6) NULL COMMENT '열람 시각(확장용)',
    clicked_at DATETIME(6) NULL COMMENT '링크 클릭 시각(확장용)',
    bounced_at DATETIME(6) NULL COMMENT '반송 시각(확장용)',
    bounce_reason VARCHAR(500) NULL COMMENT '반송 사유(확장용)',

    -- 감사 시각
    created_at DATETIME(6) NOT NULL COMMENT '이력 생성 시각',
    updated_at DATETIME(6) NULL COMMENT '이력 수정 시각',

    PRIMARY KEY (id),

    -- 관리자 목록/필터용 인덱스
    -- 최신순 목록 조회
    KEY idx_email_logs_created_at (created_at),
    -- 수신 주소 검색
    KEY idx_email_logs_recipient (recipient),
    -- 비로그인 IP 추적
    KEY idx_email_logs_request_ip (request_ip),
    -- 성공/실패 필터
    KEY idx_email_logs_status (status),
    -- 용도별 필터
    KEY idx_email_logs_purpose (purpose),
    -- 사용자별 이력
    KEY idx_email_logs_user_id (user_id),
    -- 주체(비로그인/시스템) 필터
    KEY idx_email_logs_actor_type (actor_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='메일 발송 이력';
