-- =============================================================================
-- V24: users 테이블 이중 매핑으로 생긴 죽은 중복 컬럼 제거
-- -----------------------------------------------------------------------------
-- 목적
--   - User 도메인과 UserJpaEntity 가 동일 테이블(users)에 이중 @Entity 매핑되던
--     시절 Hibernate ddl-auto 가 camelCase / snake_case 컬럼을 둘 다 만든 잔재를 정리한다.
--   - 현재 단일 매핑(UserJpaEntity)이 쓰는 컬럼만 남긴다.
--
-- 유지(살아 있는 컬럼)
--   - otp_enabled / otp_secret / otp_verified
--   - password_change_count / password_changed_at / last_password_change_date
--   - phone_number / profile_image_url
--   - kakao_user_id / kakao_access_token / kakao_refresh_token /
--     kakao_token_expires_at / kakao_notification_enabled
--   - 알림 플래그·디바이스·타임스탬프 등 camelCase 단일 컬럼
--     (emailNotificationEnabled, deviceToken, createdAt 등)
--
-- 제거(죽은 중복)
--   - otpEnabled / otpSecret / otpVerified
--   - passwordChangeCount / passwordChangedAt / lastPasswordChangeDate
--   - phoneNumber / profileImageUrl
--   - kakaoUserId / kakaoAccessToken / kakaoRefreshToken /
--     kakaoTokenExpiresAt / kakaoNotificationEnabled
--
-- 주의
--   - DROP 전에 snake_case 쪽이 NULL 이면 camelCase 값을 한 번 복사한다.
--   - 컬럼이 없는 환경(신규)에서는 no-op.
--   - 연관: UserJpaEntity, User 도메인 @Entity 제거
-- =============================================================================

SET @users_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'users'
);

-- -----------------------------------------------------------------------------
-- 데이터 보정: camelCase → snake_case (살아 있는 쪽이 NULL 일 때만)
-- -----------------------------------------------------------------------------
SET @sql := IF(@users_exists > 0
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'otpEnabled')
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'otp_enabled'),
    'UPDATE users SET otp_enabled = otpEnabled WHERE otp_enabled IS NULL AND otpEnabled IS NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@users_exists > 0
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'otpSecret')
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'otp_secret'),
    'UPDATE users SET otp_secret = otpSecret WHERE otp_secret IS NULL AND otpSecret IS NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@users_exists > 0
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'otpVerified')
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'otp_verified'),
    'UPDATE users SET otp_verified = otpVerified WHERE otp_verified IS NULL AND otpVerified IS NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@users_exists > 0
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'passwordChangeCount')
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'password_change_count'),
    'UPDATE users SET password_change_count = passwordChangeCount WHERE password_change_count IS NULL AND passwordChangeCount IS NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@users_exists > 0
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'passwordChangedAt')
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'password_changed_at'),
    'UPDATE users SET password_changed_at = passwordChangedAt WHERE password_changed_at IS NULL AND passwordChangedAt IS NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@users_exists > 0
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'lastPasswordChangeDate')
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'last_password_change_date'),
    'UPDATE users SET last_password_change_date = lastPasswordChangeDate WHERE last_password_change_date IS NULL AND lastPasswordChangeDate IS NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@users_exists > 0
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'phoneNumber')
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'phone_number'),
    'UPDATE users SET phone_number = phoneNumber WHERE phone_number IS NULL AND phoneNumber IS NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@users_exists > 0
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'profileImageUrl')
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'profile_image_url'),
    'UPDATE users SET profile_image_url = profileImageUrl WHERE profile_image_url IS NULL AND profileImageUrl IS NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@users_exists > 0
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'kakaoUserId')
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'kakao_user_id'),
    'UPDATE users SET kakao_user_id = kakaoUserId WHERE kakao_user_id IS NULL AND kakaoUserId IS NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@users_exists > 0
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'kakaoAccessToken')
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'kakao_access_token'),
    'UPDATE users SET kakao_access_token = kakaoAccessToken WHERE kakao_access_token IS NULL AND kakaoAccessToken IS NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@users_exists > 0
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'kakaoRefreshToken')
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'kakao_refresh_token'),
    'UPDATE users SET kakao_refresh_token = kakaoRefreshToken WHERE kakao_refresh_token IS NULL AND kakaoRefreshToken IS NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@users_exists > 0
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'kakaoTokenExpiresAt')
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'kakao_token_expires_at'),
    'UPDATE users SET kakao_token_expires_at = kakaoTokenExpiresAt WHERE kakao_token_expires_at IS NULL AND kakaoTokenExpiresAt IS NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@users_exists > 0
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'kakaoNotificationEnabled')
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'kakao_notification_enabled'),
    'UPDATE users SET kakao_notification_enabled = kakaoNotificationEnabled WHERE kakao_notification_enabled IS NULL AND kakaoNotificationEnabled IS NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- DROP: 죽은 camelCase 중복 컬럼
-- -----------------------------------------------------------------------------
SET @sql := IF(@users_exists > 0
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'otpEnabled'),
    'ALTER TABLE users DROP COLUMN otpEnabled', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@users_exists > 0
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'otpSecret'),
    'ALTER TABLE users DROP COLUMN otpSecret', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@users_exists > 0
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'otpVerified'),
    'ALTER TABLE users DROP COLUMN otpVerified', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@users_exists > 0
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'passwordChangeCount'),
    'ALTER TABLE users DROP COLUMN passwordChangeCount', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@users_exists > 0
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'passwordChangedAt'),
    'ALTER TABLE users DROP COLUMN passwordChangedAt', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@users_exists > 0
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'lastPasswordChangeDate'),
    'ALTER TABLE users DROP COLUMN lastPasswordChangeDate', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@users_exists > 0
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'phoneNumber'),
    'ALTER TABLE users DROP COLUMN phoneNumber', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@users_exists > 0
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'profileImageUrl'),
    'ALTER TABLE users DROP COLUMN profileImageUrl', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@users_exists > 0
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'kakaoUserId'),
    'ALTER TABLE users DROP COLUMN kakaoUserId', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@users_exists > 0
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'kakaoAccessToken'),
    'ALTER TABLE users DROP COLUMN kakaoAccessToken', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@users_exists > 0
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'kakaoRefreshToken'),
    'ALTER TABLE users DROP COLUMN kakaoRefreshToken', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@users_exists > 0
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'kakaoTokenExpiresAt'),
    'ALTER TABLE users DROP COLUMN kakaoTokenExpiresAt', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@users_exists > 0
    AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'kakaoNotificationEnabled'),
    'ALTER TABLE users DROP COLUMN kakaoNotificationEnabled', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
