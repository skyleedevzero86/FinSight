-- =============================================================================
-- V34: 뉴스 감성 상태와 임베딩 상태
-- -----------------------------------------------------------------------------
-- 목적
--   - 요약(ai_overview)과 감성 결과를 분리해 저장한다.
--   - DJL 확률, 확신도, 모델 이름, 처리 상태를 남겨 재분석할 수 있게 한다.
--   - 임베딩 색인 성공·실패와 재시도 횟수를 뉴스 행에 남긴다.
--
-- 대상
--   - MySQL finsight.news
--
-- 연관
--   - newsScrapJob 의 DJL 감성 단계, 뉴스 임베딩 단계
--   - GET /api/v1/news/{newsId}/similar
--
-- 주의
--   - 컬럼이 이미 있으면 다시 추가하지 않는다.
--   - 기존 행의 상태는 NULL, 시도 횟수는 0 이다. 다음 배치가 감성을 채운다.
-- =============================================================================

SET @sentiment_status := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'news'
      AND column_name = 'ai_sentiment_status'
);
SET @sentiment_status_sql := IF(
    @sentiment_status = 0,
    'ALTER TABLE news ADD COLUMN ai_sentiment_status varchar(16) NULL AFTER ai_sentiment_score',
    'SELECT 1'
);
PREPARE news_sentiment_status_stmt FROM @sentiment_status_sql;
EXECUTE news_sentiment_status_stmt;
DEALLOCATE PREPARE news_sentiment_status_stmt;

SET @sentiment_confidence := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'news'
      AND column_name = 'ai_sentiment_confidence'
);
SET @sentiment_confidence_sql := IF(
    @sentiment_confidence = 0,
    'ALTER TABLE news ADD COLUMN ai_sentiment_confidence double NULL AFTER ai_sentiment_status',
    'SELECT 1'
);
PREPARE news_sentiment_confidence_stmt FROM @sentiment_confidence_sql;
EXECUTE news_sentiment_confidence_stmt;
DEALLOCATE PREPARE news_sentiment_confidence_stmt;

SET @sentiment_positive := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'news'
      AND column_name = 'ai_sentiment_positive'
);
SET @sentiment_positive_sql := IF(
    @sentiment_positive = 0,
    'ALTER TABLE news ADD COLUMN ai_sentiment_positive double NULL AFTER ai_sentiment_confidence',
    'SELECT 1'
);
PREPARE news_sentiment_positive_stmt FROM @sentiment_positive_sql;
EXECUTE news_sentiment_positive_stmt;
DEALLOCATE PREPARE news_sentiment_positive_stmt;

SET @sentiment_neutral := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'news'
      AND column_name = 'ai_sentiment_neutral'
);
SET @sentiment_neutral_sql := IF(
    @sentiment_neutral = 0,
    'ALTER TABLE news ADD COLUMN ai_sentiment_neutral double NULL AFTER ai_sentiment_positive',
    'SELECT 1'
);
PREPARE news_sentiment_neutral_stmt FROM @sentiment_neutral_sql;
EXECUTE news_sentiment_neutral_stmt;
DEALLOCATE PREPARE news_sentiment_neutral_stmt;

SET @sentiment_negative := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'news'
      AND column_name = 'ai_sentiment_negative'
);
SET @sentiment_negative_sql := IF(
    @sentiment_negative = 0,
    'ALTER TABLE news ADD COLUMN ai_sentiment_negative double NULL AFTER ai_sentiment_neutral',
    'SELECT 1'
);
PREPARE news_sentiment_negative_stmt FROM @sentiment_negative_sql;
EXECUTE news_sentiment_negative_stmt;
DEALLOCATE PREPARE news_sentiment_negative_stmt;

SET @sentiment_model := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'news'
      AND column_name = 'ai_sentiment_model'
);
SET @sentiment_model_sql := IF(
    @sentiment_model = 0,
    'ALTER TABLE news ADD COLUMN ai_sentiment_model varchar(200) NULL AFTER ai_sentiment_negative',
    'SELECT 1'
);
PREPARE news_sentiment_model_stmt FROM @sentiment_model_sql;
EXECUTE news_sentiment_model_stmt;
DEALLOCATE PREPARE news_sentiment_model_stmt;

SET @sentiment_attempts := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'news'
      AND column_name = 'ai_sentiment_attempts'
);
SET @sentiment_attempts_sql := IF(
    @sentiment_attempts = 0,
    'ALTER TABLE news ADD COLUMN ai_sentiment_attempts int NOT NULL DEFAULT 0 AFTER ai_sentiment_model',
    'SELECT 1'
);
PREPARE news_sentiment_attempts_stmt FROM @sentiment_attempts_sql;
EXECUTE news_sentiment_attempts_stmt;
DEALLOCATE PREPARE news_sentiment_attempts_stmt;

SET @embedding_status := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'news'
      AND column_name = 'ai_embedding_status'
);
SET @embedding_status_sql := IF(
    @embedding_status = 0,
    'ALTER TABLE news ADD COLUMN ai_embedding_status varchar(16) NULL AFTER ai_sentiment_attempts',
    'SELECT 1'
);
PREPARE news_embedding_status_stmt FROM @embedding_status_sql;
EXECUTE news_embedding_status_stmt;
DEALLOCATE PREPARE news_embedding_status_stmt;

SET @embedding_model := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'news'
      AND column_name = 'ai_embedding_model'
);
SET @embedding_model_sql := IF(
    @embedding_model = 0,
    'ALTER TABLE news ADD COLUMN ai_embedding_model varchar(200) NULL AFTER ai_embedding_status',
    'SELECT 1'
);
PREPARE news_embedding_model_stmt FROM @embedding_model_sql;
EXECUTE news_embedding_model_stmt;
DEALLOCATE PREPARE news_embedding_model_stmt;

SET @embedding_attempts := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'news'
      AND column_name = 'ai_embedding_attempts'
);
SET @embedding_attempts_sql := IF(
    @embedding_attempts = 0,
    'ALTER TABLE news ADD COLUMN ai_embedding_attempts int NOT NULL DEFAULT 0 AFTER ai_embedding_model',
    'SELECT 1'
);
PREPARE news_embedding_attempts_stmt FROM @embedding_attempts_sql;
EXECUTE news_embedding_attempts_stmt;
DEALLOCATE PREPARE news_embedding_attempts_stmt;

SET @sentiment_index := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'news'
      AND index_name = 'idx_news_sentiment_status'
);
SET @sentiment_index_sql := IF(
    @sentiment_index = 0,
    'CREATE INDEX idx_news_sentiment_status ON news (ai_sentiment_status, ai_sentiment_attempts)',
    'SELECT 1'
);
PREPARE news_sentiment_index_stmt FROM @sentiment_index_sql;
EXECUTE news_sentiment_index_stmt;
DEALLOCATE PREPARE news_sentiment_index_stmt;
