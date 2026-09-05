-- =============================================================================
-- V20: 메인이미지 클릭 URL·노출 기간
-- -----------------------------------------------------------------------------
-- 목적
--   - mainimg_item 에 클릭 이동 URL(link_url) 과 노출 기간(notice_begin/end)을 추가한다.
--   - 관리자 UI(/admin/mainimg) 에서 제목·클릭 URL·이미지·설명·기간을 관리한다.
--   - 공개 목록(reflectOnly) 은 기간 안인 항목만 메인 슬라이더에 노출한다.
--
-- 컬럼
--   - link_url     : 이미지 클릭 시 이동 URL (비우면 클릭 없음)
--   - notice_begin : 노출 시작일 (yyyy-MM-dd, NULL/공백이면 시작 제한 없음)
--   - notice_end   : 노출 종료일 (yyyy-MM-dd, NULL/공백이면 종료 제한 없음)
--
-- 마이그레이션 주의
--   - Hibernate ddl-auto 또는 이전 수동 적용으로 컬럼이 이미 있을 수 있어
--     information_schema 로 존재 여부를 확인한 뒤 조건부 ALTER 한다.
-- =============================================================================

SET @tbl_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'mainimg_item'
);

-- -----------------------------------------------------------------------------
-- link_url
-- -----------------------------------------------------------------------------
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mainimg_item'
      AND column_name = 'link_url'
);
SET @sql := IF(
    @tbl_exists > 0 AND @col_exists = 0,
    'ALTER TABLE mainimg_item ADD COLUMN link_url VARCHAR(500) NULL COMMENT ''이미지 클릭 시 이동 URL'' AFTER description',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- notice_begin
-- -----------------------------------------------------------------------------
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mainimg_item'
      AND column_name = 'notice_begin'
);
SET @sql := IF(
    @tbl_exists > 0 AND @col_exists = 0,
    'ALTER TABLE mainimg_item ADD COLUMN notice_begin VARCHAR(20) NULL COMMENT ''노출 시작(yyyy-MM-dd)'' AFTER link_url',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- notice_end
-- -----------------------------------------------------------------------------
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mainimg_item'
      AND column_name = 'notice_end'
);
SET @sql := IF(
    @tbl_exists > 0 AND @col_exists = 0,
    'ALTER TABLE mainimg_item ADD COLUMN notice_end VARCHAR(20) NULL COMMENT ''노출 종료(yyyy-MM-dd)'' AFTER notice_begin',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 노출 기간 조회용 인덱스
-- -----------------------------------------------------------------------------
SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'mainimg_item'
      AND index_name = 'idx_mainimg_notice'
);
SET @sql := IF(
    @tbl_exists > 0 AND @idx_exists = 0,
    'ALTER TABLE mainimg_item ADD KEY idx_mainimg_notice (reflect_yn, notice_begin, notice_end)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
