-- =============================================================================
-- V39: 포트폴리오 공유 신고 상세 사유
-- -----------------------------------------------------------------------------
-- 목적
--   - 신고 팝업에서 고른 종류와 따로, 회원이 적은 사유 문장을 저장한다
-- 대상
--   - portfolio_share_report.note
-- 의존
--   - V37 portfolio_share_report.reason
-- 연관 API
--   - POST /api/v1/portfolio/shares/{shareId}/reports
-- 주의
--   - 자동 탐지 집계는 reason(종류)만 사용한다. note 는 신고 원문이다
--   - 기존 신고 행은 note 가 NULL 이다
-- =============================================================================

ALTER TABLE portfolio_share_report
    ADD COLUMN note VARCHAR(500) NULL COMMENT '회원이 입력한 신고 사유' AFTER reason;
