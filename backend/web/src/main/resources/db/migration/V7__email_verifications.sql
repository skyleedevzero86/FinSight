-- =============================================================================
-- V7: 이메일 인증 챌린지
-- -----------------------------------------------------------------------------
-- 목적
--   - 대상 테이블명 email_verifications
--   - 회원가입·아이디찾기·비밀번호찾기 등 용도의 이메일 OTP 인증을 저장한다.
--   - 인증코드는 code_hash 로만 보관하고, 평문 코드는 검증 과정에서만 잠시 기록한다.
--
-- 주요 흐름
--   1. 요청 → challenge_id + code_hash + expires_at 저장. status 는 PENDING 등
--   2. 코드 확인 → verified_at 기록
--   3. 실제 가입·재설정 완료 → consumed_at 기록. 재사용 방지
--
-- purpose 예
--   - VERIFICATION_SIGNUP
--   - VERIFICATION_FIND_EMAIL
--   - VERIFICATION_FIND_PASSWORD
-- =============================================================================

CREATE TABLE IF NOT EXISTS email_verifications (
    -- 기본키
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '이메일 인증 챌린지 PK',

    -- 용도
    purpose VARCHAR(32) NOT NULL COMMENT '인증 용도 코드 (회원가입/아이디찾기/비밀번호찾기 등)',
    purpose_label VARCHAR(50) NOT NULL COMMENT '인증 용도 표시명',

    -- 대상 / 챌린지
    email VARCHAR(255) NOT NULL COMMENT '인증 대상 이메일',
    challenge_id VARCHAR(36) NOT NULL COMMENT '챌린지 토큰(UUID). 프론트 verify URL 경로에 사용',
    code_hash VARCHAR(100) NOT NULL COMMENT '인증코드 해시 (평문 저장 금지)',
    verified_code VARCHAR(6) NULL COMMENT '검증에 성공한 코드(감사/디버그용, 선택)',
    last_entered_code VARCHAR(6) NULL COMMENT '마지막 입력 코드(실패 추적)',

    -- 요청 메타
    request_ip VARCHAR(64) NULL COMMENT '인증 요청 IP',
    request_location VARCHAR(255) NULL COMMENT 'IP 기반 대략 위치',
    requested_at DATETIME(6) NOT NULL COMMENT '인증 요청 시각',
    expires_at DATETIME(6) NOT NULL COMMENT '코드 만료 시각',
    verified_at DATETIME(6) NULL COMMENT '코드 검증 성공 시각',
    consumed_at DATETIME(6) NULL COMMENT '가입/재설정 등 최종 사용 시각',
    attempt_count INT NOT NULL DEFAULT 0 COMMENT '코드 입력 시도 횟수',
    status VARCHAR(20) NOT NULL COMMENT '챌린지 상태 (PENDING/VERIFIED/EXPIRED/CONSUMED 등)',

    -- 감사 시각
    created_at DATETIME(6) NOT NULL COMMENT '레코드 생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '레코드 수정 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uk_email_verifications_challenge (challenge_id),
    KEY idx_email_verifications_email_purpose (email, purpose, status),
    KEY idx_email_verifications_requested_at (requested_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='이메일 OTP 인증 챌린지';
