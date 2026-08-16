SET @users_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'users'
);

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'otpEnabled'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET otpEnabled = 0 WHERE otpEnabled IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN otpEnabled TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'otpVerified'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET otpVerified = 0 WHERE otpVerified IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN otpVerified TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'otp_enabled'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET otp_enabled = 0 WHERE otp_enabled IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN otp_enabled TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'otp_verified'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET otp_verified = 0 WHERE otp_verified IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN otp_verified TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'passwordChangeCount'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET passwordChangeCount = 0 WHERE passwordChangeCount IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN passwordChangeCount INT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'password_change_count'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET password_change_count = 0 WHERE password_change_count IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN password_change_count INT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
