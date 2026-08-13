-- users 테이블은 Hibernate가 만드는 경우가 많다.
-- 빈 스키마에서는 V4 시점에 테이블이 없으므로, 있을 때만 nickname을 NOT NULL로 맞춘다.

SET @users_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'users'
);

SET @update_sql := IF(
    @users_exists > 0,
    'UPDATE users SET nickname = LEFT(COALESCE(NULLIF(TRIM(nickname), ''''), username), 50) WHERE nickname IS NULL OR TRIM(nickname) = '''' OR CHAR_LENGTH(nickname) > 50',
    'SELECT 1'
);

PREPARE stmt FROM @update_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @alter_sql := IF(
    @users_exists > 0,
    'ALTER TABLE users MODIFY COLUMN nickname VARCHAR(50) NOT NULL',
    'SELECT 1'
);

PREPARE stmt FROM @alter_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
