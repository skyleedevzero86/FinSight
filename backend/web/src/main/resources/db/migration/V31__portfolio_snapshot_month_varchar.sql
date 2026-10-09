-- =============================================================================
-- V31: portfolio_snapshot.snapshot_month 타입 맞춤
-- -----------------------------------------------------------------------------
-- 목적
--   - V30의 CHAR(7)을 VARCHAR(7)로 바꿔 Hibernate validate와 맞춘다
-- 대상
--   - portfolio_snapshot.snapshot_month
-- =============================================================================

ALTER TABLE portfolio_snapshot
    MODIFY snapshot_month VARCHAR(7) NOT NULL COMMENT '기록 월';
