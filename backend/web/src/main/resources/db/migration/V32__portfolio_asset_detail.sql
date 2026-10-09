-- =============================================================================
-- V32: 포트폴리오 자산 상세·월 기록 메모
-- -----------------------------------------------------------------------------
-- 목적
--   - 자산 등록 화면의 보관처, 취득가, 수량, 단가, 메모
--   - 이번 달 기록의 메모와 확정 상태
-- 대상
--   - portfolio_asset
--   - portfolio_snapshot
-- =============================================================================

ALTER TABLE portfolio_asset
    ADD COLUMN custodian VARCHAR(80) NULL COMMENT '계좌 또는 보관처' AFTER profit_rate,
    ADD COLUMN acquisition_amount BIGINT NULL COMMENT '매수원금 또는 취득가' AFTER custodian,
    ADD COLUMN quantity DECIMAL(18, 4) NULL COMMENT '보유 수량' AFTER acquisition_amount,
    ADD COLUMN unit_price BIGINT NULL COMMENT '현재 단가' AFTER quantity,
    ADD COLUMN memo VARCHAR(500) NULL COMMENT '메모' AFTER unit_price;

ALTER TABLE portfolio_snapshot
    ADD COLUMN memo VARCHAR(500) NULL COMMENT '이번 달 메모' AFTER recorded_at,
    ADD COLUMN record_status VARCHAR(16) NOT NULL DEFAULT 'CONFIRMED' COMMENT '기록 상태' AFTER memo;
