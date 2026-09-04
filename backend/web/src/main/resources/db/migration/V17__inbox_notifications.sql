-- =============================================================================
-- V17: 인앱 알림함 (inbox)
-- -----------------------------------------------------------------------------
-- 목적
--   - 사용자별 인앱 알림 목록·읽음·삭제·미읽음 카운트
--   - 카테고리: YOUTUBE, NEWS, COMMENT, QNA, WATCHLIST, ADMIN
--   - 사용자 알림 수신 설정(유튜브/뉴스/댓글/QnA) 저장
--
-- 연관 API
--   - GET/DELETE /api/v1/inbox
--   - GET /api/v1/inbox/unread-count
--   - POST /api/v1/inbox/{id}/read, /api/v1/inbox/read-all
--   - GET/PUT /api/v1/inbox/settings
--   - POST /api/v1/admin/inbox/broadcast
-- =============================================================================

CREATE TABLE IF NOT EXISTS inbox_notification (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '인앱 알림 PK',
    recipient_user_id BIGINT NOT NULL COMMENT '수신 사용자 ID',
    category VARCHAR(32) NOT NULL COMMENT '카테고리 (YOUTUBE|NEWS|COMMENT|QNA|WATCHLIST|ADMIN)',
    actor_user_id BIGINT NULL COMMENT '행위자 사용자 ID (시스템이면 NULL)',
    actor_name VARCHAR(100) NULL COMMENT '행위자 표시명',
    actor_avatar_url VARCHAR(500) NULL COMMENT '행위자 아바타 URL',
    title VARCHAR(500) NOT NULL COMMENT '알림 제목/본문 요약',
    body VARCHAR(1000) NULL COMMENT '부가 스니펫(댓글 미리보기 등)',
    link_url VARCHAR(500) NULL COMMENT '클릭 시 이동 경로',
    ref_type VARCHAR(32) NULL COMMENT '연관 리소스 타입 (BOARD|NEWS|COMMENT|MEDIA 등)',
    ref_id BIGINT NULL COMMENT '연관 리소스 ID',
    is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT '읽음 여부 (0:미읽음, 1:읽음)',
    read_at DATETIME(3) NULL COMMENT '읽은 시각',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '소프트 삭제 (0:활성, 1:삭제)',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',

    PRIMARY KEY (id),
    KEY idx_inbox_recipient_created (recipient_user_id, deleted, created_at DESC, id DESC),
    KEY idx_inbox_recipient_unread (recipient_user_id, deleted, is_read, created_at DESC),
    KEY idx_inbox_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='사용자 인앱 알림함';

CREATE TABLE IF NOT EXISTS inbox_settings (
    user_id BIGINT NOT NULL COMMENT '사용자 ID (PK)',
    youtube_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '유튜브(미디어) 알림 수신',
    news_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '뉴스 알림 수신',
    comment_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '댓글 알림 수신',
    qna_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'QnA 알림 수신',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',

    PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='인앱 알림 카테고리 수신 설정';
