-- =============================================================================
-- V29: 공지 강조
-- -----------------------------------------------------------------------------
-- 목적
--   - boards.highlighted 로 공지 상위 강조(최대 3개)를 저장한다.
--   - 커뮤니티 공지 목록과 관리자 공지 목록이 같은 순서로 굵은 제목을 보여 준다.
--
-- 대상
--   - boards.highlighted
--
-- 연관
--   - BoardCommandService (강조 3개 제한)
--   - GET /api/v1/boards?boardType=NOTICE
--   - /community/notice, /admin/notifications
--
-- 주의
--   - 기존 행은 0(미강조)이다. 공지 외 게시판은 사용하지 않는다.
-- =============================================================================

ALTER TABLE boards
    ADD COLUMN highlighted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '공지 강조 여부';
