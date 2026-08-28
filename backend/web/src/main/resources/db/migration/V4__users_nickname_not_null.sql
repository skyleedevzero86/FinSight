-- =============================================================================
-- V4: users.nickname NOT NULL 정리
-- -----------------------------------------------------------------------------
-- 목적
--   - nickname 이 NULL·공백·과도하게 긴 값을 정리한 뒤 NOT NULL 로 고정한다.
--   - 빈 nickname 은 username 앞 50자로 채운다.
--
-- 마이그레이션 주의
--   - users 테이블은 Hibernate가 만드는 경우가 많다.
--   - 빈 스키마에서는 V4 시점에 테이블이 없을 수 있어, 있을 때만 UPDATE·ALTER 한다.
-- =============================================================================

SET @users_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'users'
);

-- NULL·공백·50자 초과 nickname 보정
SET @update_sql := IF(
    @users_exists > 0,
    'UPDATE users SET nickname = LEFT(COALESCE(NULLIF(TRIM(nickname), ''''), username), 50) WHERE nickname IS NULL OR TRIM(nickname) = '''' OR CHAR_LENGTH(nickname) > 50',
    'SELECT 1'
);

PREPARE stmt FROM @update_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- nickname NOT NULL 적용
SET @alter_sql := IF(
    @users_exists > 0,
    'ALTER TABLE users MODIFY COLUMN nickname VARCHAR(50) NOT NULL COMMENT ''사용자 닉네임''',
    'SELECT 1'
);

PREPARE stmt FROM @alter_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
