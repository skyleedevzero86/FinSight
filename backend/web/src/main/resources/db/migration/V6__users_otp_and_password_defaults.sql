-- =============================================================================
-- V6: users OTP·비밀번호 변경 카운트 컬럼 기본값 정리
-- -----------------------------------------------------------------------------
-- 목적
--   - otpEnabled / otpVerified / passwordChangeCount 계열 컬럼의 NULL 을 제거하고
--     NOT NULL + DEFAULT 로 맞춘다.
--   - Hibernate·환경에 따라 camelCase 와 snake_case 컬럼명이 혼재할 수 있어
--     두 이름을 모두 조건부 처리한다.
--
-- 대상 컬럼. 존재하는 경우만
--   - otpEnabled / otp_enabled : OTP 사용 여부. 기본 0
--   - otpVerified / otp_verified : OTP 검증 여부. 기본 0
--   - passwordChangeCount / password_change_count : 일일 변경 횟수. 기본 0
-- =============================================================================

SET @users_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'users'
);

-- -----------------------------------------------------------------------------
-- otpEnabled. camelCase
-- -----------------------------------------------------------------------------
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'otpEnabled'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET otpEnabled = 0 WHERE otpEnabled IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN otpEnabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''OTP 사용 여부''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- otpVerified. camelCase
-- -----------------------------------------------------------------------------
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'otpVerified'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET otpVerified = 0 WHERE otpVerified IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN otpVerified TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''OTP 검증 완료 여부''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- otp_enabled. snake_case
-- -----------------------------------------------------------------------------
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'otp_enabled'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET otp_enabled = 0 WHERE otp_enabled IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN otp_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''OTP 사용 여부''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- otp_verified. snake_case
-- -----------------------------------------------------------------------------
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'otp_verified'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET otp_verified = 0 WHERE otp_verified IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN otp_verified TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''OTP 검증 완료 여부''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- passwordChangeCount. camelCase
-- -----------------------------------------------------------------------------
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'passwordChangeCount'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET passwordChangeCount = 0 WHERE passwordChangeCount IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN passwordChangeCount INT NOT NULL DEFAULT 0 COMMENT ''비밀번호 변경 횟수(일일 제한용)''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- password_change_count. snake_case
-- -----------------------------------------------------------------------------
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'password_change_count'
);
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'UPDATE users SET password_change_count = 0 WHERE password_change_count IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@users_exists > 0 AND @col_exists > 0,
    'ALTER TABLE users MODIFY COLUMN password_change_count INT NOT NULL DEFAULT 0 COMMENT ''비밀번호 변경 횟수(일일 제한용)''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
