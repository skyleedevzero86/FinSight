-- =============================================================================
-- V22: 메인이미지 표시 순번(sort_order)
-- -----------------------------------------------------------------------------
-- 목적
--   - mainimg_item 에 표시 순번(sort_order)을 추가한다.
--   - 관리자 UI 에서 「순번」으로 입력하며, 비우면 서버가 다음 번호를 부여한다.
--   - 공개/관리 목록은 순번 오름차순으로 정렬한다.
--
-- 마이그레이션 주의
--   - Hibernate ddl-auto 로 컬럼이 이미 있을 수 있어 조건부 ALTER 한다.
-- =============================================================================

SET @tbl_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'mainimg_item'
);

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mainimg_item'
      AND column_name = 'sort_order'
);

SET @sql := IF(
    @tbl_exists > 0 AND @col_exists = 0,
    'ALTER TABLE mainimg_item ADD COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT ''표시 순번(작을수록 앞)'' AFTER reflect_yn',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'mainimg_item'
      AND index_name = 'idx_mainimg_sort'
);

SET @sql := IF(
    @tbl_exists > 0 AND @idx_exists = 0,
    'ALTER TABLE mainimg_item ADD KEY idx_mainimg_sort (sort_order, id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 기존 행: sort_order 가 0(미설정)이면 등록 순으로 1부터 부여
SET @r := (
    SELECT COALESCE(MAX(t.sort_order), 0)
    FROM (SELECT sort_order FROM mainimg_item WHERE sort_order > 0) t
);
UPDATE mainimg_item
SET sort_order = (@r := @r + 1)
WHERE sort_order = 0
ORDER BY created_at ASC, id ASC;
