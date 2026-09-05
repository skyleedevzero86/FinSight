-- =============================================================================
-- V21: 통합링크 이미지 경로
-- -----------------------------------------------------------------------------
-- 목적
--   - ulink_item 에 img_path 를 추가한다.
--   - 관리자 UI(/admin/ulink) 에서 구역을 텍스트/이미지로 선택한다.
--   - 이미지 모드면 파일 업로드 후 푸터에 글자 대신 이미지가 표시된다.
--
-- 컬럼
--   - img_path : 푸터 표시용 이미지 URL (이미지 모드일 때 사용)
--
-- 마이그레이션 주의
--   - Hibernate ddl-auto 로 컬럼이 이미 있을 수 있어 조건부 ALTER 한다.
-- =============================================================================

SET @tbl_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'ulink_item'
);

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ulink_item'
      AND column_name = 'img_path'
);

SET @sql := IF(
    @tbl_exists > 0 AND @col_exists = 0,
    'ALTER TABLE ulink_item ADD COLUMN img_path VARCHAR(500) NULL COMMENT ''푸터 표시용 이미지 URL'' AFTER description',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
