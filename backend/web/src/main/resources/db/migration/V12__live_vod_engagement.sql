-- V12: LIVE/VOD 즐겨찾기·별점·댓글
CREATE TABLE IF NOT EXISTS live_vod_favorites (
    id BIGINT NOT NULL AUTO_INCREMENT,
    video_id VARCHAR(32) NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_live_vod_fav_user_video (user_email, video_id),
    KEY idx_live_vod_fav_video (video_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LIVE/VOD 즐겨찾기';

CREATE TABLE IF NOT EXISTS live_vod_ratings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    video_id VARCHAR(32) NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    stars TINYINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_live_vod_rating_user_video (user_email, video_id),
    KEY idx_live_vod_rating_video (video_id),
    CONSTRAINT chk_live_vod_rating_stars CHECK (stars BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LIVE/VOD 별점(1-5)';

CREATE TABLE IF NOT EXISTS live_vod_comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    video_id VARCHAR(32) NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    author_nickname VARCHAR(100) NULL,
    content TEXT NOT NULL,
    parent_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    KEY idx_live_vod_cmt_video (video_id, created_at),
    KEY idx_live_vod_cmt_parent (parent_id),
    CONSTRAINT fk_live_vod_cmt_parent
        FOREIGN KEY (parent_id) REFERENCES live_vod_comments (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LIVE/VOD 댓글·대댓글';
