-- =============================================================================
-- V30: 나의 포트폴리오 자산·월별 기록
-- -----------------------------------------------------------------------------
-- 목적
--   - 로그인 사용자별 자산/부채 등록
--   - 월별 순자산 스냅샷 (같은 달은 한 건, 다시 기록하면 갱신)
--
-- 대상
--   - portfolio_asset
--   - portfolio_snapshot
--
-- 연관 API
--   - GET  /api/v1/portfolio
--   - POST /api/v1/portfolio/assets
--   - DELETE /api/v1/portfolio/assets/{assetId}
--   - POST /api/v1/portfolio/snapshots
-- =============================================================================

CREATE TABLE IF NOT EXISTS portfolio_asset (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '자산 PK',
    user_id BIGINT NOT NULL COMMENT '소유 사용자 ID',
    asset_name VARCHAR(80) NOT NULL COMMENT '자산 이름',
    asset_kind VARCHAR(16) NOT NULL COMMENT 'ASSET 또는 LIABILITY',
    asset_category VARCHAR(32) NOT NULL COMMENT 'STOCK_ETF|DEPOSIT|PENSION|CASH|OTHER|LIABILITY',
    valuation_amount BIGINT NOT NULL COMMENT '평가금액(원)',
    profit_rate DECIMAL(8, 2) NULL COMMENT '손익률(%), 없으면 NULL',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '등록 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',
    PRIMARY KEY (id),
    KEY idx_portfolio_asset_user_amount (user_id, valuation_amount DESC, id DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='사용자 포트폴리오 자산';

CREATE TABLE IF NOT EXISTS portfolio_snapshot (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '월별 기록 PK',
    user_id BIGINT NOT NULL COMMENT '소유 사용자 ID',
    snapshot_month CHAR(7) NOT NULL COMMENT '기록 월',
    total_assets BIGINT NOT NULL COMMENT '기록 시점 총자산(원)',
    total_liabilities BIGINT NOT NULL COMMENT '기록 시점 총부채(원)',
    net_worth BIGINT NOT NULL COMMENT '기록 시점 순자산(원)',
    recorded_at DATETIME(3) NOT NULL COMMENT '기록 시각',
    PRIMARY KEY (id),
    UNIQUE KEY uk_portfolio_snapshot_user_month (user_id, snapshot_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='사용자 포트폴리오 월별 순자산 기록';
