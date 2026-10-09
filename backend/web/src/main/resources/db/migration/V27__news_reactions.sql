-- =============================================================================
-- V27: 뉴스 좋아요/싫어요
-- -----------------------------------------------------------------------------
-- 목적
--   - 뉴스 상세의 댓글 수 옆에서 기사 단위 좋아요·싫어요를 저장한다.
--   - 사용자당 뉴스 1개만 반응을 유지한다. 같은 버튼을 다시 누르면 해제한다.
--
-- 대상
--   - MySQL finsight.news_reactions
--
-- 연관
--   - GET  /api/v1/news/{newsId}/reactions
--   - POST /api/v1/news/{newsId}/reactions
--   - news_statistics.like_count, dislike_count
--
-- 주의
--   - 댓글 작성은 기존 comments 를 그대로 쓴다.
--   - 반응 변경은 로그인 사용자만 가능하다.
-- =============================================================================

CREATE TABLE IF NOT EXISTS news_reactions (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '반응 PK',
    news_id BIGINT NOT NULL COMMENT '뉴스 ID',
    user_email VARCHAR(255) NOT NULL COMMENT '반응한 사용자 이메일',
    reaction_type VARCHAR(16) NOT NULL COMMENT 'LIKE 또는 DISLIKE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 시각',
    updated_at DATETIME(6) NULL COMMENT '변경 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uk_news_rxn_user_news (user_email, news_id),
    KEY idx_news_rxn_news_type (news_id, reaction_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='뉴스 좋아요·싫어요';
