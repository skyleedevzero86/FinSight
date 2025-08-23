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
CREATE INDEX idx_news_sentiment_score ON news(ai_sentiment_score);

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
