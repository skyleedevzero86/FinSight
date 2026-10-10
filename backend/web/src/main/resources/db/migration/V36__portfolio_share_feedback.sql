-- =============================================================================
-- V36: 포트폴리오 공유 좋아요·싫어요·신고
-- -----------------------------------------------------------------------------
-- 목적
--   - 공유 상세에서 회원 한 명당 반응 하나, 신고 하나를 저장
--   - 싫어요가 있으면 경고, 신고가 있으면 블라인드, 좋아요 10개 이상이면 정상으로 본다
-- 대상
--   - portfolio_share_reaction
--   - portfolio_share_report
-- 의존
--   - V33 portfolio_share
--   - V35 moderation_status
-- 연관 API
--   - GET  /api/v1/portfolio/shares/{shareId}
--   - POST /api/v1/portfolio/shares/{shareId}/reactions
--   - POST /api/v1/portfolio/shares/{shareId}/reports
-- 주의
--   - 같은 사용자·같은 카드는 한 행만 허용한다
--   - 삭제된 카드의 본문은 그대로 숨기고, 반응·신고는 받지 않는다
-- =============================================================================

CREATE TABLE IF NOT EXISTS portfolio_share_reaction (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '반응 PK',
    share_id BIGINT NOT NULL COMMENT 'portfolio_share.id',
    user_id BIGINT NOT NULL COMMENT '반응한 사용자 ID',
    reaction_type VARCHAR(16) NOT NULL COMMENT 'LIKE 또는 DISLIKE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '반응 시각',
    PRIMARY KEY (id),
    UNIQUE KEY uk_portfolio_share_reaction_user (share_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='포트폴리오 공유 좋아요·싫어요';

CREATE TABLE IF NOT EXISTS portfolio_share_report (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '신고 PK',
    share_id BIGINT NOT NULL COMMENT 'portfolio_share.id',
    user_id BIGINT NOT NULL COMMENT '신고한 사용자 ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '신고 시각',
    PRIMARY KEY (id),
    UNIQUE KEY uk_portfolio_share_report_user (share_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='포트폴리오 공유 신고';
