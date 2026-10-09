-- =============================================================================
-- V26: 뉴스 대표 이미지
-- -----------------------------------------------------------------------------
-- 목적
--   - news.image_url : MarketAux 등에서 받은 기사 이미지 주소를 저장한다.
--     뉴스 메인 화면이 이 주소로 기사 사진을 보여 준다.
--
-- 대상
--   - MySQL finsight.news
--
-- 연관
--   - GET /api/v1/news/latest
--   - MarketAuxNewsScrapRequester
--
-- 주의
--   - 컬럼이 이미 있으면 다시 추가하지 않는다.
--   - 기존 행은 NULL 로 두고, 이후 수집분부터 채운다.
-- =============================================================================

SET @image_col := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'news'
      AND column_name = 'image_url'
);

SET @image_sql := IF(
    @image_col = 0,
    'ALTER TABLE news ADD COLUMN image_url varchar(1000) NULL AFTER source_url',
    'SELECT 1'
);

PREPARE news_image_stmt FROM @image_sql;
EXECUTE news_image_stmt;
DEALLOCATE PREPARE news_image_stmt;
