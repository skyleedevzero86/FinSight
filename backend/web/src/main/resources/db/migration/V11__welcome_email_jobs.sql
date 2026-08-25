-- =============================================================================
-- V11: 회원가입 축하 메일 발송 큐
-- -----------------------------------------------------------------------------
-- 목적
--   - 회원가입 직후 축하 메일을 즉시가 아니라 가입 시각 기준 2시간 이내에
--     안정적으로 발송하기 위한 작업 큐다.
--   - 대상 테이블명 welcome_email_jobs
--   - 서버 재시작 후에도 PENDING 작업을 DB에서 다시 읽어 발송할 수 있다.
--
-- 발송 정책
--   - registered_at : 회원가입 또는 예약 생성 시각
--   - deadline_at   : registered_at + 2시간. 이 시각을 넘기면 EXPIRED
--   - scheduled_at  : 실제 발송 시도 예정 시각. 기본은 가입 직후 약 30초
--   - 스케줄러가 status=PENDING 이고 scheduled_at <= now <= deadline_at 인 행을 처리한다.
--
-- status 값
--   - PENDING : 발송 대기
--   - SENT    : 발송 성공
--   - EXPIRED : 2시간 기한 초과로 미발송 종료
--   - FAILED  : 재시도 한도 초과 또는 최종 실패
--
-- 제약
--   - user_id UNIQUE : 사용자당 축하 메일 작업은 1건만 유지한다.
-- =============================================================================

CREATE TABLE IF NOT EXISTS welcome_email_jobs (
    -- 기본키
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '축하 메일 작업 PK',

    -- 대상 사용자 / 시간 윈도우
    user_id BIGINT NOT NULL COMMENT '대상 사용자 ID (users.id)',
    registered_at DATETIME(6) NOT NULL COMMENT '회원가입 시각 (발송 윈도우 기준점)',
    deadline_at DATETIME(6) NOT NULL COMMENT '발송 마감 시각 (registered_at + 2시간)',
    scheduled_at DATETIME(6) NOT NULL COMMENT '다음 발송 시도 예정 시각',

    -- 처리 상태
    status VARCHAR(20) NOT NULL COMMENT '작업 상태 (PENDING|SENT|EXPIRED|FAILED)',
    attempt_count INT NOT NULL DEFAULT 0 COMMENT '발송 시도 횟수',
    last_error TEXT NULL COMMENT '마지막 실패 사유',
    sent_at DATETIME(6) NULL COMMENT '발송 성공 시각',

    -- 감사 시각
    created_at DATETIME(6) NOT NULL COMMENT '작업 생성 시각',
    updated_at DATETIME(6) NULL COMMENT '작업 수정 시각',

    PRIMARY KEY (id),

    -- 사용자당 1건만 허용
    UNIQUE KEY uk_welcome_email_jobs_user (user_id),
    -- 스케줄러: 대기열 조회
    KEY idx_welcome_email_jobs_pending (status, scheduled_at),
    -- 스케줄러: 기한 만료 정리
    KEY idx_welcome_email_jobs_deadline (status, deadline_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회원가입 축하 메일 발송 큐. 가입 후 2시간 이내';
