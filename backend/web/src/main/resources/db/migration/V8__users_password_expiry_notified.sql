-- =============================================================================
-- V8: users.password_expiry_notified_at 컬럼 추가
-- -----------------------------------------------------------------------------
-- 목적
--   - 비밀번호 만료·변경 안내 메일을 이미 보냈는지 기록한다.
--   - 비밀번호 만료 배치가 중복 안내 메일을 보내지 않도록 한다.
--
-- 마이그레이션 주의
--   - users 테이블은 Hibernate ddl-auto 로 생성되는 환경이 있어,
--     테이블·컬럼 존재 여부를 확인한 뒤 조건부 ALTER 한다.
-- =============================================================================

SET @users_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'users'
);

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'users'
      AND column_name = 'password_expiry_notified_at'
);

-- users 가 있고 컬럼이 없을 때만 추가
SET @sql := IF(
    @users_exists > 0 AND @col_exists = 0,
    'ALTER TABLE users ADD COLUMN password_expiry_notified_at DATETIME NULL COMMENT ''비밀번호 만료 안내 메일 발송 시각''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
