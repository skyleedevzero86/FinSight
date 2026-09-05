-- =============================================================================
-- V23: 통합링크 순번(sort_order) · 오픈 여부(open_yn)
-- -----------------------------------------------------------------------------
-- 목적
--   - ulink_item 에 표시 순번(sort_order)과 오픈 여부(open_yn)를 추가한다.
--   - 관리자 UI 에서 숫자 순번으로 정렬하고, 오픈/비오픈을 전환한다.
--   - 공개(푸터) 목록은 open_yn='Y' 만, 순번 오름차순으로 노출한다.
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
      AND column_name = 'sort_order'
);

SET @sql := IF(
    @tbl_exists > 0 AND @col_exists = 0,
    'ALTER TABLE ulink_item ADD COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT ''표시 순번(작을수록 앞)'' AFTER img_path',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ulink_item'
      AND column_name = 'open_yn'
);

SET @sql := IF(
    @tbl_exists > 0 AND @col_exists = 0,
    'ALTER TABLE ulink_item ADD COLUMN open_yn CHAR(1) NOT NULL DEFAULT ''Y'' COMMENT ''오픈 여부 Y/N'' AFTER sort_order',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ulink_item'
      AND index_name = 'idx_ulink_sort_open'
);

SET @sql := IF(
    @tbl_exists > 0 AND @idx_exists = 0,
    'ALTER TABLE ulink_item ADD KEY idx_ulink_sort_open (open_yn, sort_order, id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 기존 행: sort_order 가 0(미설정)이면 등록 순으로 1부터 부여
SET @r := (
    SELECT COALESCE(MAX(t.sort_order), 0)
    FROM (SELECT sort_order FROM ulink_item WHERE sort_order > 0) t
);
UPDATE ulink_item
SET sort_order = (@r := @r + 1)
WHERE sort_order = 0
ORDER BY created_at ASC, id ASC;
