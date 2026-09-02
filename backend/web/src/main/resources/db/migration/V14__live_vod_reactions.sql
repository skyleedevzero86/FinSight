-- =============================================================================
-- V14: LIVE/VOD 영상 좋아요/싫어요
-- -----------------------------------------------------------------------------
-- 사용자당 영상 1개의 반응만 유지 (LIKE | DISLIKE). 같은 버튼 재클릭 시 해제.
-- =============================================================================

CREATE TABLE IF NOT EXISTS live_vod_reactions (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '반응 PK',
    video_id VARCHAR(32) NOT NULL COMMENT 'YouTube videoId',
    user_email VARCHAR(255) NOT NULL COMMENT '반응한 사용자 이메일',
    reaction_type VARCHAR(16) NOT NULL COMMENT 'LIKE 또는 DISLIKE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 시각',
    updated_at DATETIME(6) NULL COMMENT '변경 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uk_live_vod_rxn_user_video (user_email, video_id),
    KEY idx_live_vod_rxn_video_type (video_id, reaction_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LIVE/VOD 영상 좋아요·싫어요';
