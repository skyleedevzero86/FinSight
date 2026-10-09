-- =============================================================================
-- V25: 영상 AI 보강 실패 시각, 수집 소스 거부
-- -----------------------------------------------------------------------------
-- 목적
--   - youtube_video_meta.ai_failed_at : AI 보강이 실패하면 시각을 남긴다.
--     성공(ai_generated_at)이 있으면 화면은 완료로 본다.
--   - youtube_import_sources.rejected : 검토 거부와 검토 대기를 구분한다.
--     active=1 이면 활성, rejected=1 이면 중지, 둘 다 0 이고 동기화 이력이
--     없으면 검토 대기.
--
-- 연관
--   - GET/POST /api/v1/admin/media
--   - YoutubeMediaService 보강·소스 상태 변경
--
-- 마이그레이션 주의
--   - Hibernate ddl-auto 로 테이블·컬럼이 이미 있을 수 있어 조건부 ALTER 한다.
--   - 테이블이 아직 없으면 건너뛴다.
-- =============================================================================

SET @video_tbl := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'youtube_video_meta'
);

SET @ai_failed_col := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'youtube_video_meta'
      AND column_name = 'ai_failed_at'
);

SET @sql_ai_failed := IF(
    @video_tbl > 0 AND @ai_failed_col = 0,
    'ALTER TABLE youtube_video_meta ADD COLUMN ai_failed_at DATETIME NULL COMMENT ''AI 보강 실패 시각'' AFTER ai_generated_at',
    'SELECT 1'
);
PREPARE stmt_ai_failed FROM @sql_ai_failed;
EXECUTE stmt_ai_failed;
DEALLOCATE PREPARE stmt_ai_failed;

SET @source_tbl := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'youtube_import_sources'
);

SET @rejected_col := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'youtube_import_sources'
      AND column_name = 'rejected'
);

SET @sql_rejected := IF(
    @source_tbl > 0 AND @rejected_col = 0,
    'ALTER TABLE youtube_import_sources ADD COLUMN rejected TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''검토 거부(1이면 중지)'' AFTER active',
    'SELECT 1'
);
PREPARE stmt_rejected FROM @sql_rejected;
EXECUTE stmt_rejected;
DEALLOCATE PREPARE stmt_rejected;
