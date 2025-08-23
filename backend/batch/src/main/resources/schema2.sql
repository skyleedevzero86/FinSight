-- MySQL용 대용량 더미데이터 생성 스크립트

-- 1. Spring Batch 테이블 생성 (순서대로 생성하여 외래키 제약조건 문제 방지)
CREATE TABLE IF NOT EXISTS BATCH_JOB_INSTANCE (
    JOB_INSTANCE_ID BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    VERSION BIGINT,
    JOB_NAME VARCHAR(100) NOT NULL,
    JOB_KEY VARCHAR(32) NOT NULL,
    CONSTRAINT JOB_INST_UN UNIQUE (JOB_NAME, JOB_KEY)
);

CREATE TABLE IF NOT EXISTS BATCH_JOB_EXECUTION (
    JOB_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    VERSION BIGINT,
    JOB_INSTANCE_ID BIGINT NOT NULL,
    CREATE_TIME TIMESTAMP NOT NULL,
    START_TIME TIMESTAMP NULL,
    END_TIME TIMESTAMP NULL,
    STATUS VARCHAR(10),
    EXIT_CODE VARCHAR(2500),
    EXIT_MESSAGE VARCHAR(2500),
    LAST_UPDATED TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS BATCH_JOB_EXECUTION_PARAMS (
    JOB_EXECUTION_ID BIGINT NOT NULL,
    PARAMETER_NAME VARCHAR(100) NOT NULL,
    PARAMETER_TYPE VARCHAR(100) NOT NULL,
    PARAMETER_VALUE VARCHAR(2500),
    IDENTIFYING CHAR(1) NOT NULL
);

CREATE TABLE IF NOT EXISTS BATCH_STEP_EXECUTION (
    STEP_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    VERSION BIGINT NOT NULL,
    STEP_NAME VARCHAR(100) NOT NULL,
    JOB_EXECUTION_ID BIGINT NOT NULL,
    CREATE_TIME TIMESTAMP NOT NULL,
    START_TIME TIMESTAMP NULL,
    END_TIME TIMESTAMP NULL,
    STATUS VARCHAR(10),
    COMMIT_COUNT BIGINT,
    READ_COUNT BIGINT,
    FILTER_COUNT BIGINT,
    WRITE_COUNT BIGINT,
    READ_SKIP_COUNT BIGINT,
    WRITE_SKIP_COUNT BIGINT,
    PROCESS_SKIP_COUNT BIGINT,
    ROLLBACK_COUNT BIGINT,
    EXIT_CODE VARCHAR(2500),
    EXIT_MESSAGE VARCHAR(2500),
    LAST_UPDATED TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS BATCH_STEP_EXECUTION_CONTEXT (
    STEP_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
    SHORT_CONTEXT VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT LONGTEXT
);

CREATE TABLE IF NOT EXISTS BATCH_JOB_EXECUTION_CONTEXT (
    JOB_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
    SHORT_CONTEXT VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT LONGTEXT
);

-- 외래키 제약조건 추가 (테이블 생성 후)
ALTER TABLE BATCH_JOB_EXECUTION 
ADD CONSTRAINT JOB_INST_EXEC_FK 
FOREIGN KEY (JOB_INSTANCE_ID) REFERENCES BATCH_JOB_INSTANCE(JOB_INSTANCE_ID);

ALTER TABLE BATCH_JOB_EXECUTION_PARAMS 
ADD CONSTRAINT JOB_EXEC_PARAMS_FK 
FOREIGN KEY (JOB_EXECUTION_ID) REFERENCES BATCH_JOB_EXECUTION(JOB_EXECUTION_ID);

ALTER TABLE BATCH_STEP_EXECUTION 
ADD CONSTRAINT JOB_EXEC_STEP_FK 
FOREIGN KEY (JOB_EXECUTION_ID) REFERENCES BATCH_JOB_EXECUTION(JOB_EXECUTION_ID);

ALTER TABLE BATCH_STEP_EXECUTION_CONTEXT 
ADD CONSTRAINT STEP_EXEC_CTX_FK 
FOREIGN KEY (STEP_EXECUTION_ID) REFERENCES BATCH_STEP_EXECUTION(STEP_EXECUTION_ID);

ALTER TABLE BATCH_JOB_EXECUTION_CONTEXT 
ADD CONSTRAINT JOB_EXEC_CTX_FK 
FOREIGN KEY (JOB_EXECUTION_ID) REFERENCES BATCH_JOB_EXECUTION(JOB_EXECUTION_ID);

-- 2. 사용자 테이블 생성
CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    username VARCHAR(50) NOT NULL,
    role ENUM('USER', 'PREMIUM_USER', 'ADMIN') NOT NULL DEFAULT 'USER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_role (role),
    INDEX idx_active (is_active),
    INDEX idx_created_at (created_at)
);

-- 3. 사용자 워치리스트 테이블 생성
CREATE TABLE IF NOT EXISTS user_watchlist (
    user_id BIGINT NOT NULL,
    category ENUM('SPY', 'QQQ', 'BTC', 'AAPL', 'MSFT', 'NVDA', 'GOOGL', 'META', 'TSLA', 'NONE') NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, category),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_category (category)
);

-- 4. 사용자 알림 설정 테이블 생성
CREATE TABLE IF NOT EXISTS user_notification_preferences (
    user_id BIGINT NOT NULL,
    notification_type ENUM('EMAIL', 'PUSH', 'SMS', 'IN_APP') NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, notification_type),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_notification_type (notification_type)
);

-- 5. 뉴스 테이블 생성
CREATE TABLE IF NOT EXISTS news (
    id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    news_provider ENUM('ALL', 'BLOOMBERG', 'MARKETAUX') NOT NULL,
    news_published_time TIMESTAMP NOT NULL,
    source_url VARCHAR(500) NOT NULL,
    scraped_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    original_title VARCHAR(500) NOT NULL,
    original_content LONGTEXT NOT NULL,
    ai_translated_title LONGTEXT NULL,
    ai_translated_content LONGTEXT NULL,
    ai_overview LONGTEXT NULL,
    ai_sentiment_type ENUM('POSITIVE', 'NEUTRAL', 'NEGATIVE') NULL,
    ai_sentiment_score DECIMAL(3,2) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_news_provider (news_provider),
    INDEX idx_published_time (news_published_time),
    INDEX idx_scraped_time (scraped_time),
    INDEX idx_sentiment_type (ai_sentiment_type),
    INDEX idx_overview_null (ai_overview(100)),
    INDEX idx_created_at (created_at),
    FULLTEXT idx_title_content (original_title, original_content)
);

-- 6. 뉴스 타겟 카테고리 테이블 생성
CREATE TABLE IF NOT EXISTS news_target_categories (
    news_id BIGINT NOT NULL,
    category ENUM('SPY', 'QQQ', 'BTC', 'AAPL', 'MSFT', 'NVDA', 'GOOGL', 'META', 'TSLA', 'NONE') NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (news_id, category),
    FOREIGN KEY (news_id) REFERENCES news(id) ON DELETE CASCADE,
    INDEX idx_news_id (news_id),
    INDEX idx_category (category)
);

-- ========================================
-- 대용량 더미데이터 생성 시작
-- ========================================

-- 1. 사용자 더미데이터 생성 (1,000건)
INSERT INTO users (email, password, username, role, is_active, last_login_at, created_at, updated_at)
SELECT 
    CONCAT('user', LPAD(numbers.n, 6, '0'), '@example.com') as email,
    'password123' as password,
    CONCAT('User', LPAD(numbers.n, 6, '0')) as username,
    CASE 
        WHEN numbers.n <= 3 THEN 'ADMIN'
        WHEN numbers.n <= 100 THEN 'PREMIUM_USER'
        ELSE 'USER'
    END as role,
    CASE WHEN numbers.n <= 950 THEN TRUE ELSE FALSE END as is_active,
    CASE 
        WHEN numbers.n <= 800 THEN DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
        ELSE NULL
    END as last_login_at,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY) as created_at,
    NOW() as updated_at
FROM (
    SELECT a.N + b.N * 10 + c.N * 100 + 1 as n
    FROM (SELECT 0 as N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a
    CROSS JOIN (SELECT 0 as N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b
    CROSS JOIN (SELECT 0 as N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) c
) as numbers
WHERE numbers.n <= 1000;

-- 2. 사용자 워치리스트 더미데이터 생성
INSERT INTO user_watchlist (user_id, category, created_at)
SELECT 
    u.id as user_id,
    ELT(FLOOR(RAND() * 9) + 1, 'SPY', 'QQQ', 'BTC', 'AAPL', 'MSFT', 'NVDA', 'GOOGL', 'META', 'TSLA') as category,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY) as created_at
FROM users u
WHERE u.id <= 1000;

-- 3. 사용자 알림 설정 더미데이터 생성
INSERT INTO user_notification_preferences (user_id, notification_type, created_at)
SELECT 
    u.id as user_id,
    ELT(FLOOR(RAND() * 4) + 1, 'EMAIL', 'PUSH', 'SMS', 'IN_APP') as notification_type,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY) as created_at
FROM users u
WHERE u.id <= 1000;

-- 4. 뉴스 더미데이터 생성 (5,000건)
INSERT INTO news (news_provider, news_published_time, source_url, scraped_time, original_title, original_content, ai_translated_title, ai_translated_content, ai_overview, ai_sentiment_type, ai_sentiment_score, created_at, updated_at)
SELECT 
    CASE FLOOR(RAND() * 2)
        WHEN 0 THEN 'BLOOMBERG'
        ELSE 'MARKETAUX'
    END as news_provider,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY) as news_published_time,
    CONCAT('https://example.com/news/', LPAD(numbers.n, 6, '0')) as source_url,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY) as scraped_time,
    CASE FLOOR(RAND() * 20)
        WHEN 0 THEN 'Apple Stock Surges on Strong iPhone Sales Report'
        WHEN 1 THEN 'Microsoft Cloud Services Drive Record Revenue Growth'
        WHEN 2 THEN 'NVIDIA AI Chips Demand Exceeds Supply Expectations'
        WHEN 3 THEN 'Google Parent Alphabet Reports Strong Q4 Earnings'
        WHEN 4 THEN 'Meta Platforms Faces Regulatory Challenges in Europe'
        WHEN 5 THEN 'Tesla Electric Vehicle Sales Hit New Milestone'
        WHEN 6 THEN 'Bitcoin Price Volatility Continues Amid Market Uncertainty'
        WHEN 7 THEN 'SPY ETF Shows Strong Performance in Bull Market'
        WHEN 8 THEN 'QQQ Technology Index Reaches All-Time High'
        WHEN 9 THEN 'Federal Reserve Announces Interest Rate Decision'
        WHEN 10 THEN 'Oil Prices Fluctuate on Global Supply Concerns'
        WHEN 11 THEN 'Gold Market Shows Signs of Recovery'
        WHEN 12 THEN 'Real Estate Market Faces Interest Rate Pressure'
        WHEN 13 THEN 'Healthcare Sector Innovation Drives Market Growth'
        WHEN 14 THEN 'Renewable Energy Stocks Rally on Policy Support'
        WHEN 15 THEN 'Financial Services Industry Adapts to Digital Transformation'
        WHEN 16 THEN 'Automotive Industry Shifts Toward Electric Vehicles'
        WHEN 17 THEN 'Retail Sector Struggles with Supply Chain Issues'
        WHEN 18 THEN 'Telecommunications Companies Invest in 5G Infrastructure'
        ELSE 'Global Markets React to Economic Data Release'
    END as original_title,
    CONCAT(
        'This is a comprehensive news article about the latest developments in the financial markets. ',
        'The story covers various aspects including market trends, company performance, and economic indicators. ',
        'Analysts are closely watching these developments as they could have significant implications for investors. ',
        'The market has been showing increased volatility in recent weeks, with many investors seeking safe haven assets. ',
        'This news comes at a critical time when many are reassessing their investment strategies. ',
        'The article provides detailed analysis and expert opinions on what this means for the future of the markets. ',
        'Investors are advised to carefully consider their positions and consult with financial advisors. ',
        'The market reaction to this news will be closely monitored by traders and analysts worldwide. ',
        'This development represents a significant shift in the current market landscape. ',
        'The implications of this news extend beyond immediate market movements to long-term economic trends.'
    ) as original_content,
    CASE 
        WHEN numbers.n <= 4000 THEN CONCAT('번역된 제목 - ', LPAD(numbers.n, 6, '0'))
        ELSE NULL
    END as ai_translated_title,
    CASE 
        WHEN numbers.n <= 4000 THEN CONCAT('번역된 내용 - ', LPAD(numbers.n, 6, '0'), '번째 뉴스의 상세한 분석 내용입니다. 이 뉴스는 시장에 중요한 영향을 미칠 것으로 예상됩니다.')
        ELSE NULL
    END as ai_translated_content,
    CASE 
        WHEN numbers.n <= 4000 THEN CONCAT('AI 분석 요약 - ', LPAD(numbers.n, 6, '0'), '번째 뉴스에 대한 인공지능의 종합적인 분석 결과입니다. 시장 동향과 투자자들에게 미치는 영향을 정리했습니다.')
        ELSE NULL
    END as ai_overview,
    CASE 
        WHEN numbers.n <= 4000 THEN 
            CASE FLOOR(RAND() * 3)
                WHEN 0 THEN 'POSITIVE'
                WHEN 1 THEN 'NEUTRAL'
                ELSE 'NEGATIVE'
            END
        ELSE NULL
    END as ai_sentiment_type,
    CASE 
        WHEN numbers.n <= 4000 THEN ROUND(RAND() * 1.0, 2)
        ELSE NULL
    END as ai_sentiment_score,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY) as created_at,
    NOW() as updated_at
FROM (
    SELECT a.N + b.N * 10 + c.N * 100 + d.N * 1000 + e.N * 10000 + 1 as n
    FROM (SELECT 0 as N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a
    CROSS JOIN (SELECT 0 as N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b
    CROSS JOIN (SELECT 0 as N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) c
    CROSS JOIN (SELECT 0 as N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) d
    CROSS JOIN (SELECT 0 as N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) e
) as numbers
WHERE numbers.n <= 5000;

-- 5. 뉴스 타겟 카테고리 더미데이터 생성 (각 뉴스마다 1-2개의 고유한 카테고리 할당)
INSERT INTO news_target_categories (news_id, category, created_at)
SELECT 
    news_id,
    category,
    created_at
FROM (
    SELECT 
        n.id as news_id,
        CASE FLOOR(RAND() * 10)
            WHEN 0 THEN 'SPY'
            WHEN 1 THEN 'QQQ'
            WHEN 2 THEN 'BTC'
            WHEN 3 THEN 'AAPL'
            WHEN 4 THEN 'MSFT'
            WHEN 5 THEN 'NVDA'
            WHEN 6 THEN 'GOOGL'
            WHEN 7 THEN 'META'
            WHEN 8 THEN 'TSLA'
            ELSE 'NONE'
        END as category,
        n.created_at as created_at,
        ROW_NUMBER() OVER (PARTITION BY n.id ORDER BY RAND()) as rn
    FROM news n
    WHERE n.id <= 5000 
      AND FLOOR(RAND() * 3) = 0
) ranked
WHERE rn = 1
  AND NOT EXISTS (
      SELECT 1 FROM news_target_categories existing 
      WHERE existing.news_id = ranked.news_id 
        AND existing.category = ranked.category
  );

-- 추가 카테고리 할당 (일부 뉴스에만)
INSERT INTO news_target_categories (news_id, category, created_at)
SELECT 
    n.id as news_id,
    CASE FLOOR(RAND() * 10)
        WHEN 0 THEN 'SPY'
        WHEN 1 THEN 'QQQ'
        WHEN 2 THEN 'BTC'
        WHEN 3 THEN 'AAPL'
        WHEN 4 THEN 'MSFT'
        WHEN 5 THEN 'NVDA'
        WHEN 6 THEN 'GOOGL'
        WHEN 7 THEN 'META'
        WHEN 8 THEN 'TSLA'
        ELSE 'NONE'
    END as category,
    n.created_at as created_at
FROM news n
WHERE n.id <= 5000 
  AND FLOOR(RAND() * 3) = 0  -- 1/3 확률로 추가 카테고리 할당
  AND NOT EXISTS (
      SELECT 1 FROM news_target_categories ntc 
      WHERE ntc.news_id = n.id 
        AND ntc.category = CASE FLOOR(RAND() * 10)
            WHEN 0 THEN 'SPY'
            WHEN 1 THEN 'QQQ'
            WHEN 2 THEN 'BTC'
            WHEN 3 THEN 'AAPL'
            WHEN 4 THEN 'MSFT'
            WHEN 5 THEN 'NVDA'
            WHEN 6 THEN 'GOOGL'
            WHEN 7 THEN 'META'
            WHEN 8 THEN 'TSLA'
            ELSE 'NONE'
        END
  );

-- 6. Spring Batch 작업 실행 기록 더미데이터 생성
INSERT INTO BATCH_JOB_INSTANCE (VERSION, JOB_NAME, JOB_KEY)
SELECT 
    1 as VERSION,
    CASE FLOOR(RAND() * 3)
        WHEN 0 THEN 'newsScrapJob'
        WHEN 1 THEN 'aiAnalysisJob'
        ELSE 'dataCleanupJob'
    END as JOB_NAME,
    CONCAT('JOB_KEY_', LPAD(numbers.n, 5, '0')) as JOB_KEY
FROM (
    SELECT a.N + b.N * 10 + c.N * 100 + 1 as n
    FROM (SELECT 0 as N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a
    CROSS JOIN (SELECT 0 as N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b
    CROSS JOIN (SELECT 0 as N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) c
) as numbers
WHERE numbers.n <= 100;

-- 6-1. Spring Batch 작업 실행 기록 더미데이터 생성 (BATCH_JOB_EXECUTION 테이블용)
INSERT INTO BATCH_JOB_EXECUTION (VERSION, JOB_INSTANCE_ID, CREATE_TIME, START_TIME, END_TIME, STATUS, EXIT_CODE, EXIT_MESSAGE, LAST_UPDATED)
SELECT 
    1 as VERSION,
    ji.JOB_INSTANCE_ID,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY) as CREATE_TIME,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY) as START_TIME,
    CASE WHEN RAND() > 0.3 THEN DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY) ELSE NULL END as END_TIME,
    CASE FLOOR(RAND() * 4)
        WHEN 0 THEN 'COMPLETED'
        WHEN 1 THEN 'FAILED'
        WHEN 2 THEN 'STOPPED'
        ELSE 'RUNNING'
    END as STATUS,
    CASE FLOOR(RAND() * 3)
        WHEN 0 THEN 'COMPLETED'
        WHEN 1 THEN 'FAILED'
        ELSE 'STOPPED'
    END as EXIT_CODE,
    CASE FLOOR(RAND() * 3)
        WHEN 0 THEN 'Job completed successfully'
        WHEN 1 THEN 'Job failed due to error'
        ELSE 'Job was stopped by user'
    END as EXIT_MESSAGE,
    NOW() as LAST_UPDATED
FROM BATCH_JOB_INSTANCE ji
WHERE ji.JOB_INSTANCE_ID <= 100;

-- 7. Spring Batch 작업 실행 파라미터 더미데이터 생성 (실제 존재하는 JOB_EXECUTION_ID 참조)
INSERT INTO BATCH_JOB_EXECUTION_PARAMS (JOB_EXECUTION_ID, PARAMETER_NAME, PARAMETER_TYPE, PARAMETER_VALUE, IDENTIFYING)
SELECT 
    je.JOB_EXECUTION_ID,
    CASE FLOOR(RAND() * 5)
        WHEN 0 THEN 'publishTimeAfter'
        WHEN 1 THEN 'limit'
        WHEN 2 THEN 'batchSize'
        WHEN 3 THEN 'timeout'
        ELSE 'retryCount'
    END as PARAMETER_NAME,
    CASE FLOOR(RAND() * 3)
        WHEN 0 THEN 'STRING'
        WHEN 1 THEN 'LONG'
        ELSE 'DOUBLE'
    END as PARAMETER_TYPE,
    CASE FLOOR(RAND() * 5)
        WHEN 0 THEN '2024-01-01T00:00:00'
        WHEN 1 THEN '100'
        WHEN 2 THEN '500'
        WHEN 3 THEN '30000'
        ELSE '3'
    END as PARAMETER_VALUE,
    CASE WHEN RAND() > 0.5 THEN 'Y' ELSE 'N' END as IDENTIFYING
FROM BATCH_JOB_EXECUTION je
CROSS JOIN (
    SELECT 1 as n UNION SELECT 2 UNION SELECT 3
) as param_count
WHERE je.JOB_EXECUTION_ID <= 100
  AND param_count.n <= 3;  -- 각 JOB_EXECUTION마다 최대 3개의 파라미터 생성

-- ========================================
-- 인덱스 최적화 및 통계 업데이트
-- ========================================

-- 추가 인덱스 생성
CREATE INDEX idx_news_sentiment_score ON news(ai_sentiment_score);
CREATE INDEX idx_news_provider_published ON news(news_provider, news_published_time);
CREATE INDEX idx_users_role_active ON users(role, is_active);
CREATE INDEX idx_users_created_active ON users(created_at, is_active);

-- 복합 인덱스 생성
CREATE INDEX idx_news_composite ON news(news_provider, ai_sentiment_type, news_published_time);
CREATE INDEX idx_users_composite ON users(role, is_active, created_at);

-- 통계 업데이트
ANALYZE TABLE users;
ANALYZE TABLE news;
ANALYZE TABLE user_watchlist;
ANALYZE TABLE user_notification_preferences;
ANALYZE TABLE news_target_categories;
ANALYZE TABLE BATCH_JOB_INSTANCE;
ANALYZE TABLE BATCH_JOB_EXECUTION_PARAMS;

-- ========================================
-- 데이터 검증 쿼리
-- ========================================

-- 생성된 데이터 수 확인
SELECT 
    'users' as table_name, COUNT(*) as record_count FROM users
UNION ALL
SELECT 'news' as table_name, COUNT(*) as record_count FROM news
UNION ALL
SELECT 'user_watchlist' as table_name, COUNT(*) as record_count FROM user_watchlist
UNION ALL
SELECT 'user_notification_preferences' as table_name, COUNT(*) as record_count FROM user_notification_preferences
UNION ALL
SELECT 'news_target_categories' as table_name, COUNT(*) as record_count FROM news_target_categories
UNION ALL
SELECT 'BATCH_JOB_INSTANCE' as table_name, COUNT(*) as record_count FROM BATCH_JOB_INSTANCE
UNION ALL
SELECT 'BATCH_JOB_EXECUTION_PARAMS' as table_name, COUNT(*) as record_count FROM BATCH_JOB_EXECUTION_PARAMS;

-- 샘플 데이터 확인
SELECT 'Sample Users' as info, COUNT(*) as count, MIN(email) as sample_email, MAX(created_at) as latest_created FROM users LIMIT 5;
SELECT 'Sample News' as info, COUNT(*) as count, MIN(original_title) as sample_title, MAX(created_at) as latest_created FROM news LIMIT 5;
