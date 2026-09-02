-- =============================================================================
-- V15: LIVE/VOD 댓글·대댓글 좋아요/싫어요
-- -----------------------------------------------------------------------------
-- 댓글(comment_id) 단위로 사용자당 1개 반응(LIKE|DISLIKE)만 유지한다.
-- =============================================================================

CREATE TABLE IF NOT EXISTS live_vod_comment_reactions (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '댓글 반응 PK',
    comment_id BIGINT NOT NULL COMMENT 'live_vod_comments.id',
    user_email VARCHAR(255) NOT NULL COMMENT '반응한 사용자 이메일',
    reaction_type VARCHAR(16) NOT NULL COMMENT 'LIKE 또는 DISLIKE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 시각',
    updated_at DATETIME(6) NULL COMMENT '변경 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uk_live_vod_cmt_rxn_user (user_email, comment_id),
    KEY idx_live_vod_cmt_rxn_comment_type (comment_id, reaction_type),

    CONSTRAINT fk_live_vod_cmt_rxn_comment
        FOREIGN KEY (comment_id) REFERENCES live_vod_comments (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LIVE/VOD 댓글·대댓글 좋아요·싫어요';
