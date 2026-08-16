SET @users_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'users'
);

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'telegramNotificationEnabled'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET telegramNotificationEnabled = 0 WHERE telegramNotificationEnabled IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN telegramNotificationEnabled TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'slackNotificationEnabled'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET slackNotificationEnabled = 0 WHERE slackNotificationEnabled IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN slackNotificationEnabled TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'discordNotificationEnabled'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET discordNotificationEnabled = 0 WHERE discordNotificationEnabled IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN discordNotificationEnabled TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'lineNotificationEnabled'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET lineNotificationEnabled = 0 WHERE lineNotificationEnabled IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN lineNotificationEnabled TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'webhookNotificationEnabled'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET webhookNotificationEnabled = 0 WHERE webhookNotificationEnabled IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN webhookNotificationEnabled TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'smsNotificationEnabled'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET smsNotificationEnabled = 0 WHERE smsNotificationEnabled IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN smsNotificationEnabled TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'kakaoNotificationEnabled'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET kakaoNotificationEnabled = 0 WHERE kakaoNotificationEnabled IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN kakaoNotificationEnabled TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'kakao_notification_enabled'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET kakao_notification_enabled = 0 WHERE kakao_notification_enabled IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN kakao_notification_enabled TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'pushNotificationEnabled'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET pushNotificationEnabled = 1 WHERE pushNotificationEnabled IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN pushNotificationEnabled TINYINT(1) NOT NULL DEFAULT 1', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'emailNotificationEnabled'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET emailNotificationEnabled = 1 WHERE emailNotificationEnabled IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN emailNotificationEnabled TINYINT(1) NOT NULL DEFAULT 1', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
