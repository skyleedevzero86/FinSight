-- =============================================================================
-- V16: 게시판 신고 과다 숨김 실행 이력
-- -----------------------------------------------------------------------------
-- 목적
--   - 대상 테이블명 board_moderation_run
--   - 관리자 수동 실행 또는 배치가 신고 임계값 이상 ACTIVE 글을 HIDDEN으로
--     일괄 숨김 처리할 때, 실행 메타·숨김 대상 상세를 남겨 추적한다.
--   - 관리자 UI(/admin/moderation)의 「실행 이력」「실행 상세」조회에 사용한다.
--
-- 대상 범위
--   - 커뮤니티 게시 유형만: NOTICE, FREE, QNA, COMMUNITY
--   - MEDIA(유튜브·에디터 연동 글)는 일괄 숨김·이력 대상에서 제외한다.
--
-- triggered_by
--   - MANUAL : 관리자가 API/화면에서 즉시 실행 (actor_email 기록)
--   - BATCH  : boardModerationJob 스케줄 실행 (actor_email 은 NULL 가능)
--
-- details_json
--   - 숨김 처리된 글 목록 JSON 배열
--   - 예: [{"boardId":1,"title":"...","authorEmail":"...","boardType":"FREE","reportCount":5}, ...]
--
-- 연관 API
--   - POST /api/v1/admin/boards/maintenance/hide-over-reported
--   - GET  /api/v1/admin/boards/maintenance/runs
--   - GET  /api/v1/admin/boards/maintenance/runs/{runId}
-- =============================================================================

CREATE TABLE IF NOT EXISTS board_moderation_run (
    -- 기본키
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '모더레이션 실행 이력 PK',

    -- 실행 주체
    triggered_by VARCHAR(32) NOT NULL COMMENT '실행 경로 (MANUAL: 관리자 수동, BATCH: 스케줄 배치)',
    actor_email VARCHAR(255) NULL COMMENT '수동 실행 관리자 이메일 (BATCH면 NULL 가능)',

    -- 실행 조건·결과 요약
    report_threshold INT NOT NULL COMMENT '적용한 신고 건수 임계값 (이상이면 숨김 대상)',
    hidden_count INT NOT NULL DEFAULT 0 COMMENT '이번 실행에서 HIDDEN으로 바꾼 게시글 건수',

    -- 상세 스냅샷
    details_json MEDIUMTEXT NULL COMMENT '숨김 대상 상세 JSON (boardId, title, authorEmail, boardType, reportCount)',

    -- 시각
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '실행(이력 저장) 시각',

    PRIMARY KEY (id),
    KEY idx_moderation_run_created (created_at),
    KEY idx_moderation_run_triggered (triggered_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='게시판 신고 과다 숨김 실행 이력 (수동·배치)';
