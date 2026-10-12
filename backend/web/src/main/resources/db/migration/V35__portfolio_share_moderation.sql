-- =============================================================================
-- V35: 포트폴리오 공유 카드 운영 상태
-- -----------------------------------------------------------------------------
-- 목적
--   - 관리자가 공개 공유 카드를 경고, 블라인드, 삭제로 표시
--   - 삭제된 카드는 목록에 남기되 본문을 볼 수 없고 다시 수정하지 않음
-- 대상
--   - portfolio_share.moderation_status
-- 의존
--   - V33 portfolio_share
-- 연관 API
--   - GET  /api/v1/portfolio/shares
--   - POST /api/v1/portfolio/shares/{shareId}/moderation
-- 주의
--   - 기존 행은 OPEN. PUBLIC 목록 조건은 그대로 두고 상태만 카드에 실어 보낸다
-- =============================================================================

ALTER TABLE portfolio_share
    ADD COLUMN moderation_status VARCHAR(16) NOT NULL DEFAULT 'OPEN'
        COMMENT 'OPEN, WARN, BLIND, REMOVED' AFTER cheer_count;
