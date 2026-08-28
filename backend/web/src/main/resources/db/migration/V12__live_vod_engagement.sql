-- =============================================================================
-- V12: LIVE/VOD 참여(즐겨찾기·댓글)
-- -----------------------------------------------------------------------------
-- 목적
--   - LIVE/VOD 영상(video_id) 단위로 사용자 즐겨찾기와 댓글/대댓글을 저장한다.
--   - 목록 썸네일에는 즐겨찾기 수·댓글 수를 표시하고,
--     상세 화면에서는 즐겨찾기 토글과 댓글/답글을 제공한다.
--
-- 대상 테이블
--   - live_vod_favorites : 사용자별 즐겨찾기 (user_email + video_id 유일)
--   - live_vod_comments  : 댓글·대댓글 (parent_id 로 1단계 답글)
--
-- 참여 정책
--   - video_id        : YouTube 영상 ID 문자열
--   - user_email      : 로그인 사용자 식별 (즐겨찾기·댓글 작성자)
--   - parent_id NULL  : 최상위 댓글
--   - parent_id 값    : 해당 댓글에 대한 1단계 대댓글
--   - 대댓글에 다시 답글은 애플리케이션에서 제한한다.
--
-- 제약
--   - 즐겨찾기는 (user_email, video_id) UNIQUE 로 사용자당 영상 1건만 유지한다.
--   - 댓글 parent_id 는 자기 참조 FK 이며, 부모 삭제 시 대댓글도 CASCADE 삭제한다.
-- =============================================================================

CREATE TABLE IF NOT EXISTS live_vod_favorites (
    -- 기본키
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '즐겨찾기 PK',

    -- 대상 영상 / 사용자
    video_id VARCHAR(32) NOT NULL COMMENT 'YouTube videoId',
    user_email VARCHAR(255) NOT NULL COMMENT '즐겨찾기한 사용자 이메일',

    -- 감사 시각
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '즐겨찾기 등록 시각',

    PRIMARY KEY (id),

    -- 사용자당 영상 1건만 허용
    UNIQUE KEY uk_live_vod_fav_user_video (user_email, video_id),
    -- 목록 집계: 영상별 즐겨찾기 수
    KEY idx_live_vod_fav_video (video_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LIVE/VOD 즐겨찾기. 사용자·영상당 1건';

CREATE TABLE IF NOT EXISTS live_vod_comments (
    -- 기본키
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '댓글 PK',

    -- 대상 영상 / 작성자
    video_id VARCHAR(32) NOT NULL COMMENT 'YouTube videoId',
    user_email VARCHAR(255) NOT NULL COMMENT '작성자 이메일',
    author_nickname VARCHAR(100) NULL COMMENT '작성자 닉네임(표시용)',

    -- 본문 / 대댓글
    content TEXT NOT NULL COMMENT '댓글 본문',
    parent_id BIGINT NULL COMMENT '부모 댓글 ID (NULL이면 최상위 댓글)',

    -- 감사 시각
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '작성 시각',
    updated_at DATETIME(6) NULL COMMENT '수정 시각',

    PRIMARY KEY (id),

    -- 영상별 댓글 목록/집계
    KEY idx_live_vod_cmt_video (video_id, created_at),
    -- 대댓글 조회
    KEY idx_live_vod_cmt_parent (parent_id),

    -- 부모 댓글 삭제 시 대댓글도 함께 삭제
    CONSTRAINT fk_live_vod_cmt_parent
        FOREIGN KEY (parent_id) REFERENCES live_vod_comments (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LIVE/VOD 댓글·대댓글. parent_id로 1단계 답글';
