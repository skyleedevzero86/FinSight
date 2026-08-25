-- =============================================================================
-- V5: users 알림 채널 기본값 정리
-- -----------------------------------------------------------------------------
-- 목적
--   - 각종 NotificationEnabled 컬럼의 NULL 을 제거하고 NOT NULL + DEFAULT 로 맞춘다.
--   - 이메일·푸시는 기본 ON 값 1, 그 외 채널은 기본 OFF 값 0.
--     텔레그램·슬랙 등 외부 채널이 해당한다.
--
-- 마이그레이션 주의
--   - users 테이블·컬럼이 Hibernate 로 먼저 생겼을 수 있어 존재할 때만 ALTER 한다.
--   - kakaoNotificationEnabled 와 kakao_notification_enabled 둘 다 있을 수 있어
--     camelCase / snake_case 를 각각 처리한다.
-- =============================================================================

SET @users_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'users'
);

-- -----------------------------------------------------------------------------
-- 채널별: NULL → 기본값, 이후 NOT NULL DEFAULT
-- -----------------------------------------------------------------------------

-- telegramNotificationEnabled. 기본 OFF
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'telegramNotificationEnabled'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET telegramNotificationEnabled = 0 WHERE telegramNotificationEnabled IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN telegramNotificationEnabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''텔레그램 알림 사용 여부''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- slackNotificationEnabled. 기본 OFF
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'slackNotificationEnabled'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET slackNotificationEnabled = 0 WHERE slackNotificationEnabled IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN slackNotificationEnabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''슬랙 알림 사용 여부''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- discordNotificationEnabled. 기본 OFF
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'discordNotificationEnabled'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET discordNotificationEnabled = 0 WHERE discordNotificationEnabled IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN discordNotificationEnabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''디스코드 알림 사용 여부''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- lineNotificationEnabled. 기본 OFF
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'lineNotificationEnabled'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET lineNotificationEnabled = 0 WHERE lineNotificationEnabled IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN lineNotificationEnabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''라인 알림 사용 여부''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- webhookNotificationEnabled. 기본 OFF
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'webhookNotificationEnabled'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET webhookNotificationEnabled = 0 WHERE webhookNotificationEnabled IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN webhookNotificationEnabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''웹훅 알림 사용 여부''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- smsNotificationEnabled. 기본 OFF
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'smsNotificationEnabled'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET smsNotificationEnabled = 0 WHERE smsNotificationEnabled IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN smsNotificationEnabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''SMS 알림 사용 여부''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- kakaoNotificationEnabled. 기본 OFF, camelCase
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'kakaoNotificationEnabled'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET kakaoNotificationEnabled = 0 WHERE kakaoNotificationEnabled IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN kakaoNotificationEnabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''카카오 알림 사용 여부''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- kakao_notification_enabled. 기본 OFF, snake_case
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'kakao_notification_enabled'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET kakao_notification_enabled = 0 WHERE kakao_notification_enabled IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN kakao_notification_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''카카오 알림 사용 여부''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- pushNotificationEnabled. 기본 ON
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'pushNotificationEnabled'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET pushNotificationEnabled = 1 WHERE pushNotificationEnabled IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN pushNotificationEnabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT ''푸시 알림 사용 여부(기본 ON)''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- emailNotificationEnabled. 기본 ON
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'emailNotificationEnabled'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET emailNotificationEnabled = 1 WHERE emailNotificationEnabled IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN emailNotificationEnabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT ''이메일 알림 사용 여부(기본 ON)''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
