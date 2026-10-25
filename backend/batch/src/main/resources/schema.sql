-- =============================================================================
-- schema.sql: 배치 더미 스키마, 기본 데이터, 조회 뷰
-- -----------------------------------------------------------------------------
-- 목적
--   - Spring Batch 메타데이터, 더미 사용자·뉴스, 통계용 조회 뷰를 한 번에 만든다
-- 대상
--   - BATCH_JOB_INSTANCE, BATCH_JOB_EXECUTION, BATCH_JOB_EXECUTION_PARAMS
--   - BATCH_STEP_EXECUTION, BATCH_STEP_EXECUTION_CONTEXT, BATCH_JOB_EXECUTION_CONTEXT
--   - users, user_watchlist, user_notification_preferences, news, news_target_categories
--   - v_dashboard_overview 부터 v_main_dashboard_summary 까지
-- 주의
--   - 운영 finsight 스키마가 아니다. Hibernate·Flyway 가 만드는 테이블과 컬럼이 다르다
--   - 빈 MySQL 데이터베이스에서 통계와 더미 확인용으로 실행한다
--   - 배치 메타데이터는 MySQL AUTO_INCREMENT 를 쓴다
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 배치 잡 인스턴스
-- 같은 잡 이름과 JOB_KEY 조합은 한 행만 허용한다.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS BATCH_JOB_INSTANCE (
    JOB_INSTANCE_ID BIGINT NOT NULL AUTO_INCREMENT COMMENT '잡 인스턴스 PK',
    VERSION BIGINT COMMENT '낙관적 잠금 버전',
    JOB_NAME VARCHAR(100) NOT NULL COMMENT '잡 이름',
    JOB_KEY VARCHAR(32) NOT NULL COMMENT '파라미터로 만든 잡 식별 키',
    PRIMARY KEY (JOB_INSTANCE_ID),
    CONSTRAINT JOB_INST_UN UNIQUE (JOB_NAME, JOB_KEY)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='스프링 배치 잡 인스턴스';

-- -----------------------------------------------------------------------------
-- 배치 잡 실행
-- 인스턴스 한 건이 여러 번 실행될 수 있다.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS BATCH_JOB_EXECUTION (
    JOB_EXECUTION_ID BIGINT NOT NULL AUTO_INCREMENT COMMENT '잡 실행 PK',
    VERSION BIGINT COMMENT '낙관적 잠금 버전',
    JOB_INSTANCE_ID BIGINT NOT NULL COMMENT 'BATCH_JOB_INSTANCE.JOB_INSTANCE_ID',
    CREATE_TIME TIMESTAMP NOT NULL COMMENT '실행 레코드 생성 시각',
    START_TIME TIMESTAMP NULL COMMENT '실행 시작 시각',
    END_TIME TIMESTAMP NULL COMMENT '실행 종료 시각',
    STATUS VARCHAR(10) COMMENT 'COMPLETED, FAILED, STOPPED, RUNNING',
    EXIT_CODE VARCHAR(2500) COMMENT '종료 코드',
    EXIT_MESSAGE VARCHAR(2500) COMMENT '종료 메시지',
    LAST_UPDATED TIMESTAMP NULL COMMENT '마지막 갱신 시각',
    PRIMARY KEY (JOB_EXECUTION_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='스프링 배치 잡 실행';

-- -----------------------------------------------------------------------------
-- 배치 잡 실행 파라미터
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS BATCH_JOB_EXECUTION_PARAMS (
    JOB_EXECUTION_ID BIGINT NOT NULL COMMENT 'BATCH_JOB_EXECUTION.JOB_EXECUTION_ID',
    PARAMETER_NAME VARCHAR(100) NOT NULL COMMENT '파라미터 이름',
    PARAMETER_TYPE VARCHAR(100) NOT NULL COMMENT 'STRING, LONG, DOUBLE 등',
    PARAMETER_VALUE VARCHAR(2500) COMMENT '파라미터 값',
    IDENTIFYING CHAR(1) NOT NULL COMMENT '잡 인스턴스 식별에 쓰면 Y, 아니면 N'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='스프링 배치 잡 실행 파라미터';

-- -----------------------------------------------------------------------------
-- 배치 스텝 실행
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS BATCH_STEP_EXECUTION (
    STEP_EXECUTION_ID BIGINT NOT NULL AUTO_INCREMENT COMMENT '스텝 실행 PK',
    VERSION BIGINT NOT NULL COMMENT '낙관적 잠금 버전',
    STEP_NAME VARCHAR(100) NOT NULL COMMENT '스텝 이름',
    JOB_EXECUTION_ID BIGINT NOT NULL COMMENT 'BATCH_JOB_EXECUTION.JOB_EXECUTION_ID',
    CREATE_TIME TIMESTAMP NOT NULL COMMENT '스텝 레코드 생성 시각',
    START_TIME TIMESTAMP NULL COMMENT '스텝 시작 시각',
    END_TIME TIMESTAMP NULL COMMENT '스텝 종료 시각',
    STATUS VARCHAR(10) COMMENT '스텝 상태',
    COMMIT_COUNT BIGINT COMMENT '커밋 횟수',
    READ_COUNT BIGINT COMMENT '읽은 건수',
    FILTER_COUNT BIGINT COMMENT '필터로 걸러진 건수',
    WRITE_COUNT BIGINT COMMENT '쓴 건수',
    READ_SKIP_COUNT BIGINT COMMENT '읽기 건너뛴 건수',
    WRITE_SKIP_COUNT BIGINT COMMENT '쓰기 건너뛴 건수',
    PROCESS_SKIP_COUNT BIGINT COMMENT '처리 중 건너뛴 건수',
    ROLLBACK_COUNT BIGINT COMMENT '롤백 횟수',
    EXIT_CODE VARCHAR(2500) COMMENT '종료 코드',
    EXIT_MESSAGE VARCHAR(2500) COMMENT '종료 메시지',
    LAST_UPDATED TIMESTAMP NULL COMMENT '마지막 갱신 시각',
    PRIMARY KEY (STEP_EXECUTION_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='스프링 배치 스텝 실행';

-- -----------------------------------------------------------------------------
-- 스텝 실행 컨텍스트
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS BATCH_STEP_EXECUTION_CONTEXT (
    STEP_EXECUTION_ID BIGINT NOT NULL COMMENT 'BATCH_STEP_EXECUTION.STEP_EXECUTION_ID',
    SHORT_CONTEXT VARCHAR(2500) NOT NULL COMMENT '짧은 컨텍스트 문자열',
    SERIALIZED_CONTEXT LONGTEXT COMMENT '직렬화한 스텝 실행 컨텍스트',
    PRIMARY KEY (STEP_EXECUTION_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='스프링 배치 스텝 실행 컨텍스트';

-- -----------------------------------------------------------------------------
-- 잡 실행 컨텍스트
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS BATCH_JOB_EXECUTION_CONTEXT (
    JOB_EXECUTION_ID BIGINT NOT NULL COMMENT 'BATCH_JOB_EXECUTION.JOB_EXECUTION_ID',
    SHORT_CONTEXT VARCHAR(2500) NOT NULL COMMENT '짧은 컨텍스트 문자열',
    SERIALIZED_CONTEXT LONGTEXT COMMENT '직렬화한 잡 실행 컨텍스트',
    PRIMARY KEY (JOB_EXECUTION_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='스프링 배치 잡 실행 컨텍스트';

-- -----------------------------------------------------------------------------
-- 배치 테이블 외래키
-- 부모 테이블을 만든 뒤에 건다.
-- -----------------------------------------------------------------------------
ALTER TABLE BATCH_JOB_EXECUTION
    ADD CONSTRAINT JOB_INST_EXEC_FK
    FOREIGN KEY (JOB_INSTANCE_ID) REFERENCES BATCH_JOB_INSTANCE (JOB_INSTANCE_ID);

ALTER TABLE BATCH_JOB_EXECUTION_PARAMS
    ADD CONSTRAINT JOB_EXEC_PARAMS_FK
    FOREIGN KEY (JOB_EXECUTION_ID) REFERENCES BATCH_JOB_EXECUTION (JOB_EXECUTION_ID);

ALTER TABLE BATCH_STEP_EXECUTION
    ADD CONSTRAINT JOB_EXEC_STEP_FK
    FOREIGN KEY (JOB_EXECUTION_ID) REFERENCES BATCH_JOB_EXECUTION (JOB_EXECUTION_ID);

ALTER TABLE BATCH_STEP_EXECUTION_CONTEXT
    ADD CONSTRAINT STEP_EXEC_CTX_FK
    FOREIGN KEY (STEP_EXECUTION_ID) REFERENCES BATCH_STEP_EXECUTION (STEP_EXECUTION_ID);

ALTER TABLE BATCH_JOB_EXECUTION_CONTEXT
    ADD CONSTRAINT JOB_EXEC_CTX_FK
    FOREIGN KEY (JOB_EXECUTION_ID) REFERENCES BATCH_JOB_EXECUTION (JOB_EXECUTION_ID);

-- -----------------------------------------------------------------------------
-- 더미 사용자
-- 운영 사용자 테이블이 아니다.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '사용자 PK',
    email VARCHAR(100) NOT NULL COMMENT '로그인 이메일',
    password VARCHAR(255) NOT NULL COMMENT '비밀번호',
    username VARCHAR(50) NOT NULL COMMENT '사용자 이름',
    role ENUM('USER', 'PREMIUM_USER', 'ADMIN') NOT NULL DEFAULT 'USER' COMMENT 'USER, PREMIUM_USER, ADMIN',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '사용 여부. 1이면 사용',
    last_login_at TIMESTAMP NULL COMMENT '마지막 로그인 시각',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 시각',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    KEY idx_email (email),
    KEY idx_role (role),
    KEY idx_active (is_active),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='더미 사용자';

-- -----------------------------------------------------------------------------
-- 더미 관심 종목
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_watchlist (
    user_id BIGINT NOT NULL COMMENT 'users.id',
    category ENUM('SPY', 'QQQ', 'BTC', 'AAPL', 'MSFT', 'NVDA', 'GOOGL', 'META', 'TSLA', 'NONE') NOT NULL COMMENT '관심 종목 코드',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 시각',
    PRIMARY KEY (user_id, category),
    CONSTRAINT fk_user_watchlist_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    KEY idx_user_id (user_id),
    KEY idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='더미 사용자 관심 종목';

-- -----------------------------------------------------------------------------
-- 더미 알림 수단
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_notification_preferences (
    user_id BIGINT NOT NULL COMMENT 'users.id',
    notification_type ENUM('EMAIL', 'PUSH', 'SMS', 'IN_APP') NOT NULL COMMENT 'EMAIL, PUSH, SMS, IN_APP',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 시각',
    PRIMARY KEY (user_id, notification_type),
    CONSTRAINT fk_user_notification_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    KEY idx_user_id (user_id),
    KEY idx_notification_type (notification_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='더미 사용자 알림 수단';

-- -----------------------------------------------------------------------------
-- 더미 뉴스
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS news (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '뉴스 PK',
    news_provider ENUM('ALL', 'BLOOMBERG', 'MARKETAUX') NOT NULL COMMENT '뉴스 제공처',
    news_published_time TIMESTAMP NOT NULL COMMENT '원문 게시 시각',
    source_url VARCHAR(500) NOT NULL COMMENT '원문 주소',
    scraped_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수집 시각',
    original_title VARCHAR(500) NOT NULL COMMENT '원문 제목',
    original_content LONGTEXT NOT NULL COMMENT '원문 본문',
    ai_translated_title LONGTEXT NULL COMMENT '번역 제목',
    ai_translated_content LONGTEXT NULL COMMENT '번역 본문',
    ai_overview LONGTEXT NULL COMMENT 'AI 요약',
    ai_sentiment_type ENUM('POSITIVE', 'NEUTRAL', 'NEGATIVE') NULL COMMENT 'POSITIVE, NEUTRAL, NEGATIVE',
    ai_sentiment_score DECIMAL(3,2) NULL COMMENT '감정 점수 0.00~1.00',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 시각',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    PRIMARY KEY (id),
    KEY idx_news_provider (news_provider),
    KEY idx_published_time (news_published_time),
    KEY idx_scraped_time (scraped_time),
    KEY idx_sentiment_type (ai_sentiment_type),
    KEY idx_overview_null (ai_overview(100)),
    KEY idx_created_at (created_at),
    FULLTEXT KEY idx_title_content (original_title, original_content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='더미 뉴스';

-- -----------------------------------------------------------------------------
-- 더미 뉴스 종목
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS news_target_categories (
    news_id BIGINT NOT NULL COMMENT 'news.id',
    category ENUM('SPY', 'QQQ', 'BTC', 'AAPL', 'MSFT', 'NVDA', 'GOOGL', 'META', 'TSLA', 'NONE') NOT NULL COMMENT '관련 종목 코드',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록 시각',
    PRIMARY KEY (news_id, category),
    CONSTRAINT fk_news_target_news FOREIGN KEY (news_id) REFERENCES news (id) ON DELETE CASCADE,
    KEY idx_news_id (news_id),
    KEY idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='더미 뉴스 관련 종목';

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

-- ========================================
-- FinSight 시스템 통계 및 분석 뷰 생성
-- ========================================

-- 1. 대시보드용 통계 뷰 (화면 표시용)
-- ========================================

-- 1-1. 전체 시스템 현황 대시보드 뷰
CREATE OR REPLACE VIEW v_dashboard_overview AS
SELECT 
    (SELECT COUNT(*) FROM users) as total_users,
    (SELECT COUNT(*) FROM users WHERE is_active = TRUE) as active_users,
    (SELECT COUNT(*) FROM users WHERE role = 'PREMIUM_USER' OR role = 'ADMIN') as premium_users,
    (SELECT COUNT(*) FROM news) as total_news,
    (SELECT COUNT(*) FROM news WHERE ai_overview IS NOT NULL) as analyzed_news,
    (SELECT COUNT(*) FROM news WHERE ai_sentiment_type IS NOT NULL) as sentiment_analyzed_news,
    (SELECT COUNT(*) FROM BATCH_JOB_INSTANCE) as total_batch_jobs,
    (SELECT COUNT(*) FROM BATCH_JOB_EXECUTION WHERE STATUS = 'COMPLETED') as completed_batch_jobs,
    (SELECT COUNT(*) FROM BATCH_JOB_EXECUTION WHERE STATUS = 'FAILED') as failed_batch_jobs,
    (SELECT COUNT(*) FROM user_watchlist) as total_watchlist_items,
    (SELECT COUNT(*) FROM user_notification_preferences) as total_notification_preferences;

-- 1-2. 사용자 통계 뷰
CREATE OR REPLACE VIEW v_user_statistics AS
SELECT 
    role,
    COUNT(*) as user_count,
    COUNT(CASE WHEN is_active = TRUE THEN 1 END) as active_count,
    COUNT(CASE WHEN is_active = FALSE THEN 1 END) as inactive_count,
    COUNT(CASE WHEN last_login_at IS NOT NULL THEN 1 END) as logged_in_count,
    COUNT(CASE WHEN last_login_at IS NULL THEN 1 END) as never_logged_in_count,
    AVG(CASE WHEN last_login_at IS NOT NULL THEN DATEDIFF(NOW(), last_login_at) END) as avg_days_since_last_login,
    MIN(created_at) as first_user_created,
    MAX(created_at) as last_user_created
FROM users 
GROUP BY role
ORDER BY user_count DESC;

-- 1-3. 뉴스 통계 뷰
CREATE OR REPLACE VIEW v_news_statistics AS
SELECT 
    news_provider,
    COUNT(*) as total_news,
    COUNT(CASE WHEN ai_translated_title IS NOT NULL THEN 1 END) as translated_title_count,
    COUNT(CASE WHEN ai_translated_content IS NOT NULL THEN 1 END) as translated_content_count,
    COUNT(CASE WHEN ai_overview IS NOT NULL THEN 1 END) as overview_count,
    COUNT(CASE WHEN ai_sentiment_type IS NOT NULL THEN 1 END) as sentiment_count,
    COUNT(CASE WHEN ai_sentiment_score IS NOT NULL THEN 1 END) as sentiment_score_count,
    AVG(CASE WHEN ai_sentiment_score IS NOT NULL THEN ai_sentiment_score END) as avg_sentiment_score,
    MIN(news_published_time) as earliest_news,
    MAX(news_published_time) as latest_news,
    MIN(scraped_time) as earliest_scraped,
    MAX(scraped_time) as latest_scraped
FROM news 
GROUP BY news_provider
ORDER BY total_news DESC;

-- 1-4. 감정 분석 통계 뷰
CREATE OR REPLACE VIEW v_sentiment_statistics AS
SELECT 
    ai_sentiment_type,
    COUNT(*) as news_count,
    AVG(ai_sentiment_score) as avg_score,
    MIN(ai_sentiment_score) as min_score,
    MAX(ai_sentiment_score) as max_score,
    STDDEV(ai_sentiment_score) as score_stddev,
    COUNT(CASE WHEN ai_sentiment_score >= 0.7 THEN 1 END) as high_positive_count,
    COUNT(CASE WHEN ai_sentiment_score <= 0.3 THEN 1 END) as high_negative_count,
    COUNT(CASE WHEN ai_sentiment_score BETWEEN 0.4 AND 0.6 THEN 1 END) as neutral_count
FROM news 
WHERE ai_sentiment_type IS NOT NULL AND ai_sentiment_score IS NOT NULL
GROUP BY ai_sentiment_type
ORDER BY news_count DESC;

-- 1-5. 카테고리별 뉴스 통계 뷰
CREATE OR REPLACE VIEW v_category_news_statistics AS
SELECT 
    ntc.category,
    COUNT(DISTINCT n.id) as news_count,
    COUNT(CASE WHEN n.ai_overview IS NOT NULL THEN 1 END) as analyzed_count,
    COUNT(CASE WHEN n.ai_sentiment_type IS NOT NULL THEN 1 END) as sentiment_count,
    AVG(CASE WHEN n.ai_sentiment_score IS NOT NULL THEN n.ai_sentiment_score END) as avg_sentiment_score,
    COUNT(CASE WHEN n.ai_sentiment_type = 'POSITIVE' THEN 1 END) as positive_count,
    COUNT(CASE WHEN n.ai_sentiment_type = 'NEUTRAL' THEN 1 END) as neutral_count,
    COUNT(CASE WHEN n.ai_sentiment_type = 'NEGATIVE' THEN 1 END) as negative_count
FROM news_target_categories ntc
JOIN news n ON ntc.news_id = n.id
GROUP BY ntc.category
ORDER BY news_count DESC;

-- 1-6. 일별 뉴스 생성 통계 뷰
CREATE OR REPLACE VIEW v_daily_news_statistics AS
SELECT 
    DATE(created_at) as news_date,
    COUNT(*) as total_news,
    COUNT(CASE WHEN ai_overview IS NOT NULL THEN 1 END) as analyzed_news,
    COUNT(CASE WHEN ai_sentiment_type IS NOT NULL THEN 1 END) as sentiment_news,
    AVG(CASE WHEN ai_sentiment_score IS NOT NULL THEN ai_sentiment_score END) as avg_sentiment_score,
    COUNT(CASE WHEN news_provider = 'BLOOMBERG' THEN 1 END) as bloomberg_count,
    COUNT(CASE WHEN news_provider = 'MARKETAUX' THEN 1 END) as marketaux_count
FROM news 
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY DATE(created_at)
ORDER BY news_date DESC;

-- 1-7. Spring Batch 작업 통계 뷰
CREATE OR REPLACE VIEW v_batch_job_statistics AS
SELECT 
    ji.JOB_NAME,
    COUNT(*) as total_executions,
    COUNT(CASE WHEN je.STATUS = 'COMPLETED' THEN 1 END) as completed_count,
    COUNT(CASE WHEN je.STATUS = 'FAILED' THEN 1 END) as failed_count,
    COUNT(CASE WHEN je.STATUS = 'RUNNING' THEN 1 END) as running_count,
    COUNT(CASE WHEN je.STATUS = 'STOPPED' THEN 1 END) as stopped_count,
    AVG(CASE WHEN je.START_TIME IS NOT NULL AND je.END_TIME IS NOT NULL 
        THEN TIMESTAMPDIFF(SECOND, je.START_TIME, je.END_TIME) END) as avg_execution_time_seconds,
    MIN(je.CREATE_TIME) as first_execution,
    MAX(je.CREATE_TIME) as last_execution
FROM BATCH_JOB_INSTANCE ji
LEFT JOIN BATCH_JOB_EXECUTION je ON ji.JOB_INSTANCE_ID = je.JOB_INSTANCE_ID
GROUP BY ji.JOB_NAME
ORDER BY total_executions DESC;

-- 2. 엑셀 출력용 상세 통계 뷰
-- ========================================

-- 2-1. 사용자 상세 정보 엑셀 출력용 뷰
CREATE OR REPLACE VIEW v_excel_user_details AS
SELECT 
    u.id as user_id,
    u.email,
    u.username,
    u.role,
    u.is_active,
    u.last_login_at,
    u.created_at,
    u.updated_at,
    DATEDIFF(NOW(), u.created_at) as days_since_registration,
    DATEDIFF(NOW(), u.last_login_at) as days_since_last_login,
    (SELECT COUNT(*) FROM user_watchlist uw WHERE uw.user_id = u.id) as watchlist_count,
    (SELECT COUNT(*) FROM user_notification_preferences unp WHERE unp.user_id = u.id) as notification_preference_count
FROM users u
ORDER BY u.created_at DESC;

-- 2-2. 뉴스 상세 정보 엑셀 출력용 뷰
CREATE OR REPLACE VIEW v_excel_news_details AS
SELECT 
    n.id as news_id,
    n.news_provider,
    n.news_published_time,
    n.source_url,
    n.scraped_time,
    n.original_title,
    LEFT(n.original_content, 200) as content_preview,
    n.ai_translated_title,
    LEFT(n.ai_translated_content, 200) as translated_content_preview,
    LEFT(n.ai_overview, 300) as overview_preview,
    n.ai_sentiment_type,
    n.ai_sentiment_score,
    n.created_at,
    n.updated_at,
    DATEDIFF(NOW(), n.news_published_time) as days_since_published,
    DATEDIFF(NOW(), n.scraped_time) as days_since_scraped,
    (SELECT GROUP_CONCAT(ntc.category SEPARATOR ', ') FROM news_target_categories ntc WHERE ntc.news_id = n.id) as target_categories
FROM news n
ORDER BY n.created_at DESC;

-- 2-3. 감정 분석 상세 정보 엑셀 출력용 뷰
CREATE OR REPLACE VIEW v_excel_sentiment_details AS
SELECT 
    n.id as news_id,
    n.news_provider,
    n.news_published_time,
    n.original_title,
    LEFT(n.original_content, 150) as content_preview,
    n.ai_sentiment_type,
    n.ai_sentiment_score,
    CASE 
        WHEN n.ai_sentiment_score >= 0.8 THEN '매우 긍정적'
        WHEN n.ai_sentiment_score >= 0.6 THEN '긍정적'
        WHEN n.ai_sentiment_score >= 0.4 THEN '중립'
        WHEN n.ai_sentiment_score >= 0.2 THEN '부정적'
        ELSE '매우 부정적'
    END as sentiment_level,
    n.created_at,
    (SELECT GROUP_CONCAT(ntc.category SEPARATOR ', ') FROM news_target_categories ntc WHERE ntc.news_id = n.id) as target_categories
FROM news n
WHERE n.ai_sentiment_type IS NOT NULL AND n.ai_sentiment_score IS NOT NULL
ORDER BY n.ai_sentiment_score DESC;

-- 2-4. 카테고리별 상세 통계 엑셀 출력용 뷰
CREATE OR REPLACE VIEW v_excel_category_details AS
SELECT 
    ntc.category,
    n.id as news_id,
    n.news_provider,
    n.news_published_time,
    n.original_title,
    LEFT(n.original_content, 150) as content_preview,
    n.ai_sentiment_type,
    n.ai_sentiment_score,
    n.ai_overview IS NOT NULL as has_overview,
    n.created_at,
    DATEDIFF(NOW(), n.news_published_time) as days_since_published
FROM news_target_categories ntc
JOIN news n ON ntc.news_id = n.id
ORDER BY ntc.category, n.created_at DESC;

-- 2-5. Spring Batch 작업 상세 정보 엑셀 출력용 뷰
CREATE OR REPLACE VIEW v_excel_batch_details AS
SELECT 
    ji.JOB_INSTANCE_ID,
    ji.JOB_NAME,
    ji.JOB_KEY,
    je.JOB_EXECUTION_ID,
    je.CREATE_TIME,
    je.START_TIME,
    je.END_TIME,
    je.STATUS,
    je.EXIT_CODE,
    je.EXIT_MESSAGE,
    je.LAST_UPDATED,
    CASE 
        WHEN je.START_TIME IS NOT NULL AND je.END_TIME IS NOT NULL 
        THEN TIMESTAMPDIFF(SECOND, je.START_TIME, je.END_TIME)
        ELSE NULL 
    END as execution_time_seconds,
    CASE 
        WHEN je.START_TIME IS NOT NULL AND je.END_TIME IS NOT NULL 
        THEN CONCAT(
            FLOOR(TIMESTAMPDIFF(SECOND, je.START_TIME, je.END_TIME) / 3600), 'h ',
            FLOOR((TIMESTAMPDIFF(SECOND, je.START_TIME, je.END_TIME) % 3600) / 60), 'm ',
            TIMESTAMPDIFF(SECOND, je.START_TIME, je.END_TIME) % 60, 's'
        )
        ELSE NULL 
    END as execution_time_formatted
FROM BATCH_JOB_INSTANCE ji
LEFT JOIN BATCH_JOB_EXECUTION je ON ji.JOB_INSTANCE_ID = je.JOB_INSTANCE_ID
ORDER BY je.CREATE_TIME DESC;

-- 3. 실시간 모니터링용 뷰
-- ========================================

-- 3-1. 실시간 시스템 상태 모니터링 뷰
CREATE OR REPLACE VIEW v_realtime_system_status AS
SELECT 
    'System Status' as metric_name,
    CASE 
        WHEN (SELECT COUNT(*) FROM BATCH_JOB_EXECUTION WHERE STATUS = 'RUNNING') > 0 THEN 'RUNNING'
        WHEN (SELECT COUNT(*) FROM BATCH_JOB_EXECUTION WHERE STATUS = 'FAILED' AND END_TIME >= DATE_SUB(NOW(), INTERVAL 1 HOUR)) > 0 THEN 'WARNING'
        ELSE 'HEALTHY'
    END as status,
    (SELECT COUNT(*) FROM BATCH_JOB_EXECUTION WHERE STATUS = 'RUNNING') as running_jobs,
    (SELECT COUNT(*) FROM BATCH_JOB_EXECUTION WHERE STATUS = 'FAILED' AND END_TIME >= DATE_SUB(NOW(), INTERVAL 1 HOUR)) as recent_failures,
    NOW() as last_updated;

-- 3-2. 최근 24시간 뉴스 처리 현황 뷰
CREATE OR REPLACE VIEW v_recent_24h_news_status AS
SELECT 
    'Last 24 Hours' as time_period,
    COUNT(*) as total_news,
    COUNT(CASE WHEN ai_overview IS NOT NULL THEN 1 END) as processed_news,
    COUNT(CASE WHEN ai_overview IS NULL THEN 1 END) as pending_news,
    ROUND(COUNT(CASE WHEN ai_overview IS NOT NULL THEN 1 END) * 100.0 / COUNT(*), 2) as processing_rate_percent,
    COUNT(CASE WHEN ai_sentiment_type IS NOT NULL THEN 1 END) as sentiment_processed,
    ROUND(COUNT(CASE WHEN ai_sentiment_type IS NOT NULL THEN 1 END) * 100.0 / COUNT(*), 2) as sentiment_rate_percent
FROM news 
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR);

-- 4. 성능 분석용 뷰
-- ========================================

-- 4-1. 뉴스 처리 성능 분석 뷰
CREATE OR REPLACE VIEW v_news_processing_performance AS
SELECT 
    DATE(created_at) as processing_date,
    COUNT(*) as total_news,
    COUNT(CASE WHEN ai_overview IS NOT NULL THEN 1 END) as overview_processed,
    COUNT(CASE WHEN ai_sentiment_type IS NOT NULL THEN 1 END) as sentiment_processed,
    ROUND(COUNT(CASE WHEN ai_overview IS NOT NULL THEN 1 END) * 100.0 / COUNT(*), 2) as overview_success_rate,
    ROUND(COUNT(CASE WHEN ai_sentiment_type IS NOT NULL THEN 1 END) * 100.0 / COUNT(*), 2) as sentiment_success_rate,
    AVG(CASE WHEN ai_sentiment_score IS NOT NULL THEN ai_sentiment_score END) as avg_sentiment_score,
    STDDEV(CASE WHEN ai_sentiment_score IS NOT NULL THEN ai_sentiment_score END) as sentiment_score_stddev
FROM news 
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY DATE(created_at)
ORDER BY processing_date DESC;

-- 4-2. 사용자 활동 성능 분석 뷰
CREATE OR REPLACE VIEW v_user_activity_performance AS
SELECT 
    DATE(u.created_at) as registration_date,
    COUNT(*) as new_users,
    COUNT(CASE WHEN u.last_login_at IS NOT NULL THEN 1 END) as active_users,
    ROUND(COUNT(CASE WHEN u.last_login_at IS NOT NULL THEN 1 END) * 100.0 / COUNT(*), 2) as activation_rate,
    AVG(CASE WHEN u.last_login_at IS NOT NULL THEN DATEDIFF(u.last_login_at, u.created_at) END) as avg_days_to_first_login,
    COUNT(CASE WHEN u.role = 'PREMIUM_USER' OR u.role = 'ADMIN' THEN 1 END) as premium_users
FROM users u
WHERE u.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY DATE(u.created_at)
ORDER BY registration_date DESC;

-- 5. 데이터 품질 검증용 뷰
-- ========================================

-- 5-1. 뉴스 데이터 품질 검증 뷰
CREATE OR REPLACE VIEW v_news_data_quality AS
SELECT 
    'Data Quality Check' as check_type,
    COUNT(*) as total_records,
    COUNT(CASE WHEN original_title IS NOT NULL AND LENGTH(TRIM(original_title)) > 0 THEN 1 END) as valid_titles,
    COUNT(CASE WHEN original_content IS NOT NULL AND LENGTH(TRIM(original_content)) > 0 THEN 1 END) as valid_contents,
    COUNT(CASE WHEN source_url IS NOT NULL AND LENGTH(TRIM(source_url)) > 0 THEN 1 END) as valid_urls,
    COUNT(CASE WHEN news_published_time IS NOT NULL THEN 1 END) as valid_publish_times,
    COUNT(CASE WHEN ai_overview IS NOT NULL AND LENGTH(TRIM(ai_overview)) > 0 THEN 1 END) as valid_overviews,
    COUNT(CASE WHEN ai_sentiment_type IS NOT NULL THEN 1 END) as valid_sentiment_types,
    COUNT(CASE WHEN ai_sentiment_score IS NOT NULL AND ai_sentiment_score >= 0 AND ai_sentiment_score <= 1 THEN 1 END) as valid_sentiment_scores,
    ROUND(COUNT(CASE WHEN ai_overview IS NOT NULL AND LENGTH(TRIM(ai_overview)) > 0 THEN 1 END) * 100.0 / COUNT(*), 2) as overview_completion_rate,
    ROUND(COUNT(CASE WHEN ai_sentiment_type IS NOT NULL THEN 1 END) * 100.0 / COUNT(*), 2) as sentiment_completion_rate
FROM news;

-- 5-2. 사용자 데이터 품질 검증 뷰
CREATE OR REPLACE VIEW v_user_data_quality AS
SELECT 
    'User Data Quality Check' as check_type,
    COUNT(*) as total_users,
    COUNT(CASE WHEN email IS NOT NULL AND LENGTH(TRIM(email)) > 0 THEN 1 END) as valid_emails,
    COUNT(CASE WHEN username IS NOT NULL AND LENGTH(TRIM(username)) > 0 THEN 1 END) as valid_usernames,
    COUNT(CASE WHEN password IS NOT NULL AND LENGTH(TRIM(password)) > 0 THEN 1 END) as valid_passwords,
    COUNT(CASE WHEN role IS NOT NULL THEN 1 END) as valid_roles,
    COUNT(CASE WHEN created_at IS NOT NULL THEN 1 END) as valid_creation_dates,
    COUNT(CASE WHEN email REGEXP '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$' THEN 1 END) as properly_formatted_emails,
    COUNT(CASE WHEN LENGTH(TRIM(username)) >= 3 THEN 1 END) as adequate_username_length
FROM users;

-- 6. 통계 요약 뷰 (메인 대시보드용)
-- ========================================

-- 6-1. 메인 대시보드 통계 요약 뷰
CREATE OR REPLACE VIEW v_main_dashboard_summary AS
SELECT 
    'System Overview' as section,
    (SELECT COUNT(*) FROM users) as total_users,
    (SELECT COUNT(*) FROM news) as total_news,
    (SELECT COUNT(*) FROM BATCH_JOB_INSTANCE) as total_batch_jobs,
    (SELECT COUNT(*) FROM user_watchlist) as total_watchlist_items
UNION ALL
SELECT 
    'Processing Status' as section,
    (SELECT COUNT(*) FROM news WHERE ai_overview IS NOT NULL) as processed_news,
    (SELECT COUNT(*) FROM news WHERE ai_sentiment_type IS NOT NULL) as sentiment_processed,
    (SELECT COUNT(*) FROM BATCH_JOB_EXECUTION WHERE STATUS = 'COMPLETED') as completed_jobs,
    (SELECT COUNT(*) FROM BATCH_JOB_EXECUTION WHERE STATUS = 'FAILED') as failed_jobs
UNION ALL
SELECT 
    'User Activity' as section,
    (SELECT COUNT(*) FROM users WHERE is_active = TRUE) as active_users,
    (SELECT COUNT(*) FROM users WHERE last_login_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)) as recent_active_users,
    (SELECT COUNT(*) FROM users WHERE role = 'PREMIUM_USER' OR role = 'ADMIN') as premium_users,
    (SELECT COUNT(*) FROM user_notification_preferences) as notification_users;

-- 7. 인덱스 및 성능 최적화
-- ========================================

-- 통계 뷰 성능 향상을 위한 추가 인덱스
-- MySQL에서는 IF NOT EXISTS를 지원하지 않으므로, 인덱스가 이미 존재하는 경우 에러가 발생할 수 있습니다.
-- 필요한 경우 수동으로 인덱스를 생성하거나, 에러 발생 시 무시하세요.

-- 뉴스 테이블 인덱스
CREATE INDEX idx_news_created_at_date ON news(created_at);
CREATE INDEX idx_news_published_date ON news(news_published_time);
-- idx_news_sentiment_score 는 앞의 인덱스 구간에서 이미 만든다.

-- 사용자 테이블 인덱스
CREATE INDEX idx_users_created_date ON users(created_at);
CREATE INDEX idx_users_last_login ON users(last_login_at);

-- Spring Batch 테이블 인덱스
CREATE INDEX idx_batch_job_execution_status ON BATCH_JOB_EXECUTION(STATUS, END_TIME);
CREATE INDEX idx_batch_job_execution_create_time ON BATCH_JOB_EXECUTION(CREATE_TIME);

-- 복합 인덱스 생성
CREATE INDEX idx_news_provider_created_sentiment ON news(news_provider, created_at, ai_sentiment_type);
CREATE INDEX idx_users_role_active_created ON users(role, is_active, created_at);
CREATE INDEX idx_batch_job_name_status ON BATCH_JOB_INSTANCE(JOB_NAME, JOB_INSTANCE_ID);

-- 통계 테이블 분석
ANALYZE TABLE users;
ANALYZE TABLE news;
ANALYZE TABLE user_watchlist;
ANALYZE TABLE user_notification_preferences;
ANALYZE TABLE news_target_categories;
ANALYZE TABLE BATCH_JOB_INSTANCE;
ANALYZE TABLE BATCH_JOB_EXECUTION;

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

-- 뷰 사용 예시 및 테스트 쿼리
-- ========================================

-- 대시보드 통계 확인
-- SELECT * FROM v_dashboard_overview;

-- 사용자 통계 확인
-- SELECT * FROM v_user_statistics;

-- 뉴스 통계 확인
-- SELECT * FROM v_news_statistics;

-- 감정 분석 통계 확인
-- SELECT * FROM v_sentiment_statistics;

-- 카테고리별 뉴스 통계 확인
-- SELECT * FROM v_category_news_statistics;

-- 일별 뉴스 통계 확인
-- SELECT * FROM v_daily_news_statistics;

-- Spring Batch 작업 통계 확인
-- SELECT * FROM v_batch_job_statistics;

-- 실시간 시스템 상태 확인
-- SELECT * FROM v_realtime_system_status;

-- 최근 24시간 뉴스 처리 현황 확인
-- SELECT * FROM v_recent_24h_news_status;

-- 메인 대시보드 요약 확인
-- SELECT * FROM v_main_dashboard_summary;
