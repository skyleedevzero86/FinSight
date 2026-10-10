-- =============================================================================
-- V38: 포트폴리오 공유 공개 Snapshot과 댓글
-- -----------------------------------------------------------------------------
-- 목적
--   - 공유 당시 공개하기로 한 금액·구성·월 기록만 JSON으로 고정한다
--   - 공유 상세에서 회원이 짧은 댓글을 남긴다
-- 대상
--   - portfolio_share.public_detail
--   - portfolio_share_comment
-- 의존
--   - V33 portfolio_share
-- 연관 API
--   - GET  /api/v1/portfolio/shares/{shareId}
--   - GET  /api/v1/portfolio/shares/{shareId}/comments
--   - POST /api/v1/portfolio/shares/{shareId}/comments
-- 주의
--   - public_detail에는 계좌번호, 보관처, 주민번호, 전화번호를 넣지 않는다
--   - 기존 행은 public_detail이 비어 있고, 상세는 저장된 공개 문구로 합성한다
--   - 작성자 이름은 상세 응답에서 첫 글자만 보여 준다
--   - 삭제된 공유에는 댓글을 받지 않는다
-- =============================================================================

ALTER TABLE portfolio_share
    ADD COLUMN public_detail TEXT NULL COMMENT '공개 Snapshot JSON. 계좌·보관처는 제외' AFTER message;

CREATE TABLE IF NOT EXISTS portfolio_share_comment (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '댓글 PK',
    share_id BIGINT NOT NULL COMMENT 'portfolio_share.id',
    user_id BIGINT NOT NULL COMMENT '작성 사용자 ID',
    author_name VARCHAR(50) NOT NULL COMMENT '작성 당시 이름',
    content VARCHAR(500) NOT NULL COMMENT '댓글 본문',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '작성 시각',
    PRIMARY KEY (id),
    KEY idx_portfolio_share_comment_share (share_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='포트폴리오 공유 댓글';
