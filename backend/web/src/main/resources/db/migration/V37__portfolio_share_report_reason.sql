-- =============================================================================
-- V37: 포트폴리오 공유 신고 사유
-- -----------------------------------------------------------------------------
-- 목적
--   - 상세 화면에서 고른 신고 사유를 저장
--   - 관리자 자동 탐지가 이 사유와 공개 글 판별을 함께 집계
-- 대상
--   - portfolio_share_report.reason
-- 의존
--   - V36 portfolio_share_report
-- 연관 API
--   - POST /api/v1/portfolio/shares/{shareId}/reports
--   - GET  /api/v1/admin/portfolio/detections
-- 주의
--   - 기존 신고 행은 reason 이 NULL. 집계에서는 비어 있는 사유를 세지 않는다
-- =============================================================================

ALTER TABLE portfolio_share_report
    ADD COLUMN reason VARCHAR(40) NULL COMMENT '신고 사유. 오픈채팅 URL, 과장 수익문구, 리딩방 패턴, 투자 사기, 개인정보, 광고' AFTER user_id;
