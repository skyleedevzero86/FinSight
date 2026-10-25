-- =============================================================================
-- finsight_constraints: 컬럼, 제약, 인덱스
-- -----------------------------------------------------------------------------
-- 목적
--   - 이미 있는 테이블에 컬럼, 제약, 인덱스를 맞춘다
-- 대상
--   - users, news, boards, youtube_video_meta, youtube_import_sources
--   - mainimg_item, ulink_item, portfolio_asset, portfolio_snapshot
--   - portfolio_share, portfolio_share_report, editor_assets
-- 주의
--   - NOT NULL 로 바꾸기 전에 필요한 값 보정은 이 파일에서 그 제약 바로 앞에 둔다
--   - users 이중 컬럼을 지우기 전에 값을 옮기는 문장도 이 파일에 있다
--   - news.source_url 유니크 제약은 중복 행을 지운 뒤에만 걸 수 있어 finsight_data.sql 에 있다
--   - Hibernate 가 이미 컬럼을 만든 경우는 조건부 ALTER 로 건너뛴다
-- =============================================================================

-- 업로드 시각 기준 조회·배치 정리용
CREATE INDEX idx_editor_assets_uploaded_at ON editor_assets (uploaded_at);

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

-- =============================================================================
-- V20: 메인이미지 클릭 URL·노출 기간
-- -----------------------------------------------------------------------------
-- 목적
--   - mainimg_item 에 클릭 이동 URL(link_url) 과 노출 기간(notice_begin/end)을 추가한다.
--   - 관리자 UI(/admin/mainimg) 에서 제목·클릭 URL·이미지·설명·기간을 관리한다.
--   - 공개 목록(reflectOnly) 은 기간 안인 항목만 메인 슬라이더에 노출한다.
--
-- 컬럼
--   - link_url     : 이미지 클릭 시 이동 URL (비우면 클릭 없음)
--   - notice_begin : 노출 시작일 (yyyy-MM-dd, NULL/공백이면 시작 제한 없음)
--   - notice_end   : 노출 종료일 (yyyy-MM-dd, NULL/공백이면 종료 제한 없음)
--
-- 마이그레이션 주의
--   - Hibernate ddl-auto 또는 이전 수동 적용으로 컬럼이 이미 있을 수 있어
--     information_schema 로 존재 여부를 확인한 뒤 조건부 ALTER 한다.
-- =============================================================================

SET @tbl_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'mainimg_item'
);

-- -----------------------------------------------------------------------------
-- link_url
-- -----------------------------------------------------------------------------
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mainimg_item'
      AND column_name = 'link_url'
);
SET @sql := IF(
    @tbl_exists > 0 AND @col_exists = 0,
    'ALTER TABLE mainimg_item ADD COLUMN link_url VARCHAR(500) NULL COMMENT ''이미지 클릭 시 이동 URL'' AFTER description',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- notice_begin
-- -----------------------------------------------------------------------------
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mainimg_item'
      AND column_name = 'notice_begin'
);
SET @sql := IF(
    @tbl_exists > 0 AND @col_exists = 0,
    'ALTER TABLE mainimg_item ADD COLUMN notice_begin VARCHAR(20) NULL COMMENT ''노출 시작(yyyy-MM-dd)'' AFTER link_url',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- notice_end
-- -----------------------------------------------------------------------------
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mainimg_item'
      AND column_name = 'notice_end'
);
SET @sql := IF(
    @tbl_exists > 0 AND @col_exists = 0,
    'ALTER TABLE mainimg_item ADD COLUMN notice_end VARCHAR(20) NULL COMMENT ''노출 종료(yyyy-MM-dd)'' AFTER notice_begin',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 노출 기간 조회용 인덱스
-- -----------------------------------------------------------------------------
SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'mainimg_item'
      AND index_name = 'idx_mainimg_notice'
);
SET @sql := IF(
    @tbl_exists > 0 AND @idx_exists = 0,
    'ALTER TABLE mainimg_item ADD KEY idx_mainimg_notice (reflect_yn, notice_begin, notice_end)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =============================================================================
-- V21: 통합링크 이미지 경로
-- -----------------------------------------------------------------------------
-- 목적
--   - ulink_item 에 img_path 를 추가한다.
--   - 관리자 UI(/admin/ulink) 에서 구역을 텍스트/이미지로 선택한다.
--   - 이미지 모드면 파일 업로드 후 푸터에 글자 대신 이미지가 표시된다.
--
-- 컬럼
--   - img_path : 푸터 표시용 이미지 URL (이미지 모드일 때 사용)
--
-- 마이그레이션 주의
--   - Hibernate ddl-auto 로 컬럼이 이미 있을 수 있어 조건부 ALTER 한다.
-- =============================================================================

SET @tbl_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'ulink_item'
);

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ulink_item'
      AND column_name = 'img_path'
);

SET @sql := IF(
    @tbl_exists > 0 AND @col_exists = 0,
    'ALTER TABLE ulink_item ADD COLUMN img_path VARCHAR(500) NULL COMMENT ''푸터 표시용 이미지 URL'' AFTER description',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =============================================================================
-- V22: 메인이미지 표시 순번(sort_order)
-- -----------------------------------------------------------------------------
-- 목적
--   - mainimg_item 에 표시 순번(sort_order)을 추가한다.
--   - 관리자 UI 에서 「순번」으로 입력하며, 비우면 서버가 다음 번호를 부여한다.
--   - 공개/관리 목록은 순번 오름차순으로 정렬한다.
--
-- 마이그레이션 주의
--   - Hibernate ddl-auto 로 컬럼이 이미 있을 수 있어 조건부 ALTER 한다.
-- =============================================================================

SET @tbl_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'mainimg_item'
);

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mainimg_item'
      AND column_name = 'sort_order'
);

SET @sql := IF(
    @tbl_exists > 0 AND @col_exists = 0,
    'ALTER TABLE mainimg_item ADD COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT ''표시 순번(작을수록 앞)'' AFTER reflect_yn',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'mainimg_item'
      AND index_name = 'idx_mainimg_sort'
);

SET @sql := IF(
    @tbl_exists > 0 AND @idx_exists = 0,
    'ALTER TABLE mainimg_item ADD KEY idx_mainimg_sort (sort_order, id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =============================================================================
-- V23: 통합링크 순번(sort_order) · 오픈 여부(open_yn)
-- -----------------------------------------------------------------------------
-- 목적
--   - ulink_item 에 표시 순번(sort_order)과 오픈 여부(open_yn)를 추가한다.
--   - 관리자 UI 에서 숫자 순번으로 정렬하고, 오픈/비오픈을 전환한다.
--   - 공개(푸터) 목록은 open_yn='Y' 만, 순번 오름차순으로 노출한다.
--
-- 마이그레이션 주의
--   - Hibernate ddl-auto 로 컬럼이 이미 있을 수 있어 조건부 ALTER 한다.
-- =============================================================================

SET @tbl_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'ulink_item'
);

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ulink_item'
      AND column_name = 'sort_order'
);

SET @sql := IF(
    @tbl_exists > 0 AND @col_exists = 0,
    'ALTER TABLE ulink_item ADD COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT ''표시 순번(작을수록 앞)'' AFTER img_path',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ulink_item'
      AND column_name = 'open_yn'
);

SET @sql := IF(
    @tbl_exists > 0 AND @col_exists = 0,
    'ALTER TABLE ulink_item ADD COLUMN open_yn CHAR(1) NOT NULL DEFAULT ''Y'' COMMENT ''오픈 여부 Y/N'' AFTER sort_order',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ulink_item'
      AND index_name = 'idx_ulink_sort_open'
);

SET @sql := IF(
    @tbl_exists > 0 AND @idx_exists = 0,
    'ALTER TABLE ulink_item ADD KEY idx_ulink_sort_open (open_yn, sort_order, id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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

-- =============================================================================
-- V25: 영상 AI 보강 실패 시각, 수집 소스 거부
-- -----------------------------------------------------------------------------
-- 목적
--   - youtube_video_meta.ai_failed_at : AI 보강이 실패하면 시각을 남긴다.
--     성공(ai_generated_at)이 있으면 화면은 완료로 본다.
--   - youtube_import_sources.rejected : 검토 거부와 검토 대기를 구분한다.
--     active=1 이면 활성, rejected=1 이면 중지, 둘 다 0 이고 동기화 이력이
--     없으면 검토 대기.
--
-- 연관
--   - GET/POST /api/v1/admin/media
--   - YoutubeMediaService 보강·소스 상태 변경
--
-- 마이그레이션 주의
--   - Hibernate ddl-auto 로 테이블·컬럼이 이미 있을 수 있어 조건부 ALTER 한다.
--   - 테이블이 아직 없으면 건너뛴다.
-- =============================================================================

SET @video_tbl := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'youtube_video_meta'
);

SET @ai_failed_col := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'youtube_video_meta'
      AND column_name = 'ai_failed_at'
);

SET @sql_ai_failed := IF(
    @video_tbl > 0 AND @ai_failed_col = 0,
    'ALTER TABLE youtube_video_meta ADD COLUMN ai_failed_at DATETIME NULL COMMENT ''AI 보강 실패 시각'' AFTER ai_generated_at',
    'SELECT 1'
);
PREPARE stmt_ai_failed FROM @sql_ai_failed;
EXECUTE stmt_ai_failed;
DEALLOCATE PREPARE stmt_ai_failed;

SET @source_tbl := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'youtube_import_sources'
);

SET @rejected_col := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'youtube_import_sources'
      AND column_name = 'rejected'
);

SET @sql_rejected := IF(
    @source_tbl > 0 AND @rejected_col = 0,
    'ALTER TABLE youtube_import_sources ADD COLUMN rejected TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''검토 거부(1이면 중지)'' AFTER active',
    'SELECT 1'
);
PREPARE stmt_rejected FROM @sql_rejected;
EXECUTE stmt_rejected;
DEALLOCATE PREPARE stmt_rejected;

-- =============================================================================
-- V26: 뉴스 대표 이미지
-- -----------------------------------------------------------------------------
-- 목적
--   - news.image_url : MarketAux 등에서 받은 기사 이미지 주소를 저장한다.
--     뉴스 메인 화면이 이 주소로 기사 사진을 보여 준다.
--
-- 대상
--   - MySQL finsight.news
--
-- 연관
--   - GET /api/v1/news/latest
--   - MarketAuxNewsScrapRequester
--
-- 주의
--   - 컬럼이 이미 있으면 다시 추가하지 않는다.
--   - 기존 행은 NULL 로 두고, 이후 수집분부터 채운다.
-- =============================================================================

SET @image_col := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'news'
      AND column_name = 'image_url'
);

SET @image_sql := IF(
    @image_col = 0,
    'ALTER TABLE news ADD COLUMN image_url varchar(1000) NULL AFTER source_url',
    'SELECT 1'
);

PREPARE news_image_stmt FROM @image_sql;
EXECUTE news_image_stmt;
DEALLOCATE PREPARE news_image_stmt;

-- =============================================================================
-- V28: 뉴스 제공자 Yahoo Finance
-- -----------------------------------------------------------------------------
-- 목적
--   - news.news_provider 에 ALPHA_VANTAGE, YAHOO_FINANCE 를 추가한다.
--   - MarketAux 외 뉴스 API 기사를 같은 news 테이블에 저장하기 위함이다.
--
-- 대상
--   - MySQL finsight.news.news_provider
--
-- 연관
--   - YahooFinanceNewsScrapRequester
--   - GET /api/v1/news/latest?provider=YAHOO_FINANCE
--
-- 주의
--   - 기존 ALL, BLOOMBERG, MARKETAUX 값은 유지한다.
-- =============================================================================

ALTER TABLE news
    MODIFY COLUMN news_provider
        ENUM('ALL', 'BLOOMBERG', 'MARKETAUX', 'ALPHA_VANTAGE', 'YAHOO_FINANCE')
        NOT NULL COMMENT '뉴스 제공자';

-- =============================================================================
-- V29: 공지 강조
-- -----------------------------------------------------------------------------
-- 목적
--   - boards.highlighted 로 공지 상위 강조(최대 3개)를 저장한다.
--   - 커뮤니티 공지 목록과 관리자 공지 목록이 같은 순서로 굵은 제목을 보여 준다.
--
-- 대상
--   - boards.highlighted
--
-- 연관
--   - BoardCommandService (강조 3개 제한)
--   - GET /api/v1/boards?boardType=NOTICE
--   - /community/notice, /admin/notifications
--
-- 주의
--   - 기존 행은 0(미강조)이다. 공지 외 게시판은 사용하지 않는다.
-- =============================================================================

ALTER TABLE boards
    ADD COLUMN highlighted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '공지 강조 여부';

-- =============================================================================
-- V31: portfolio_snapshot.snapshot_month 타입 맞춤
-- -----------------------------------------------------------------------------
-- 목적
--   - V30의 CHAR(7)을 VARCHAR(7)로 바꿔 Hibernate validate와 맞춘다
-- 대상
--   - portfolio_snapshot.snapshot_month
-- =============================================================================

ALTER TABLE portfolio_snapshot
    MODIFY snapshot_month VARCHAR(7) NOT NULL COMMENT '기록 월';

-- =============================================================================
-- V32: 포트폴리오 자산 상세·월 기록 메모
-- -----------------------------------------------------------------------------
-- 목적
--   - 자산 등록 화면의 보관처, 취득가, 수량, 단가, 메모
--   - 이번 달 기록의 메모와 확정 상태
-- 대상
--   - portfolio_asset
--   - portfolio_snapshot
-- =============================================================================

ALTER TABLE portfolio_asset
    ADD COLUMN custodian VARCHAR(80) NULL COMMENT '계좌 또는 보관처' AFTER profit_rate,
    ADD COLUMN acquisition_amount BIGINT NULL COMMENT '매수원금 또는 취득가' AFTER custodian,
    ADD COLUMN quantity DECIMAL(18, 4) NULL COMMENT '보유 수량' AFTER acquisition_amount,
    ADD COLUMN unit_price BIGINT NULL COMMENT '현재 단가' AFTER quantity,
    ADD COLUMN memo VARCHAR(500) NULL COMMENT '메모' AFTER unit_price;

ALTER TABLE portfolio_snapshot
    ADD COLUMN memo VARCHAR(500) NULL COMMENT '이번 달 메모' AFTER recorded_at,
    ADD COLUMN record_status VARCHAR(16) NOT NULL DEFAULT 'CONFIRMED' COMMENT '기록 상태' AFTER memo;

-- =============================================================================
-- V34: 뉴스 감성 상태와 임베딩 상태
-- -----------------------------------------------------------------------------
-- 목적
--   - 요약(ai_overview)과 감성 결과를 분리해 저장한다.
--   - DJL 확률, 확신도, 모델 이름, 처리 상태를 남겨 재분석할 수 있게 한다.
--   - 임베딩 색인 성공·실패와 재시도 횟수를 뉴스 행에 남긴다.
--
-- 대상
--   - MySQL finsight.news
--
-- 연관
--   - newsScrapJob 의 DJL 감성 단계, 뉴스 임베딩 단계
--   - GET /api/v1/news/{newsId}/similar
--
-- 주의
--   - 컬럼이 이미 있으면 다시 추가하지 않는다.
--   - 기존 행의 상태는 NULL, 시도 횟수는 0 이다. 다음 배치가 감성을 채운다.
-- =============================================================================

SET @sentiment_status := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'news'
      AND column_name = 'ai_sentiment_status'
);
SET @sentiment_status_sql := IF(
    @sentiment_status = 0,
    'ALTER TABLE news ADD COLUMN ai_sentiment_status varchar(16) NULL AFTER ai_sentiment_score',
    'SELECT 1'
);
PREPARE news_sentiment_status_stmt FROM @sentiment_status_sql;
EXECUTE news_sentiment_status_stmt;
DEALLOCATE PREPARE news_sentiment_status_stmt;

SET @sentiment_confidence := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'news'
      AND column_name = 'ai_sentiment_confidence'
);
SET @sentiment_confidence_sql := IF(
    @sentiment_confidence = 0,
    'ALTER TABLE news ADD COLUMN ai_sentiment_confidence double NULL AFTER ai_sentiment_status',
    'SELECT 1'
);
PREPARE news_sentiment_confidence_stmt FROM @sentiment_confidence_sql;
EXECUTE news_sentiment_confidence_stmt;
DEALLOCATE PREPARE news_sentiment_confidence_stmt;

SET @sentiment_positive := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'news'
      AND column_name = 'ai_sentiment_positive'
);
SET @sentiment_positive_sql := IF(
    @sentiment_positive = 0,
    'ALTER TABLE news ADD COLUMN ai_sentiment_positive double NULL AFTER ai_sentiment_confidence',
    'SELECT 1'
);
PREPARE news_sentiment_positive_stmt FROM @sentiment_positive_sql;
EXECUTE news_sentiment_positive_stmt;
DEALLOCATE PREPARE news_sentiment_positive_stmt;

SET @sentiment_neutral := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'news'
      AND column_name = 'ai_sentiment_neutral'
);
SET @sentiment_neutral_sql := IF(
    @sentiment_neutral = 0,
    'ALTER TABLE news ADD COLUMN ai_sentiment_neutral double NULL AFTER ai_sentiment_positive',
    'SELECT 1'
);
PREPARE news_sentiment_neutral_stmt FROM @sentiment_neutral_sql;
EXECUTE news_sentiment_neutral_stmt;
DEALLOCATE PREPARE news_sentiment_neutral_stmt;

SET @sentiment_negative := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'news'
      AND column_name = 'ai_sentiment_negative'
);
SET @sentiment_negative_sql := IF(
    @sentiment_negative = 0,
    'ALTER TABLE news ADD COLUMN ai_sentiment_negative double NULL AFTER ai_sentiment_neutral',
    'SELECT 1'
);
PREPARE news_sentiment_negative_stmt FROM @sentiment_negative_sql;
EXECUTE news_sentiment_negative_stmt;
DEALLOCATE PREPARE news_sentiment_negative_stmt;

SET @sentiment_model := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'news'
      AND column_name = 'ai_sentiment_model'
);
SET @sentiment_model_sql := IF(
    @sentiment_model = 0,
    'ALTER TABLE news ADD COLUMN ai_sentiment_model varchar(200) NULL AFTER ai_sentiment_negative',
    'SELECT 1'
);
PREPARE news_sentiment_model_stmt FROM @sentiment_model_sql;
EXECUTE news_sentiment_model_stmt;
DEALLOCATE PREPARE news_sentiment_model_stmt;

SET @sentiment_attempts := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'news'
      AND column_name = 'ai_sentiment_attempts'
);
SET @sentiment_attempts_sql := IF(
    @sentiment_attempts = 0,
    'ALTER TABLE news ADD COLUMN ai_sentiment_attempts int NOT NULL DEFAULT 0 AFTER ai_sentiment_model',
    'SELECT 1'
);
PREPARE news_sentiment_attempts_stmt FROM @sentiment_attempts_sql;
EXECUTE news_sentiment_attempts_stmt;
DEALLOCATE PREPARE news_sentiment_attempts_stmt;

SET @embedding_status := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'news'
      AND column_name = 'ai_embedding_status'
);
SET @embedding_status_sql := IF(
    @embedding_status = 0,
    'ALTER TABLE news ADD COLUMN ai_embedding_status varchar(16) NULL AFTER ai_sentiment_attempts',
    'SELECT 1'
);
PREPARE news_embedding_status_stmt FROM @embedding_status_sql;
EXECUTE news_embedding_status_stmt;
DEALLOCATE PREPARE news_embedding_status_stmt;

SET @embedding_model := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'news'
      AND column_name = 'ai_embedding_model'
);
SET @embedding_model_sql := IF(
    @embedding_model = 0,
    'ALTER TABLE news ADD COLUMN ai_embedding_model varchar(200) NULL AFTER ai_embedding_status',
    'SELECT 1'
);
PREPARE news_embedding_model_stmt FROM @embedding_model_sql;
EXECUTE news_embedding_model_stmt;
DEALLOCATE PREPARE news_embedding_model_stmt;

SET @embedding_attempts := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'news'
      AND column_name = 'ai_embedding_attempts'
);
SET @embedding_attempts_sql := IF(
    @embedding_attempts = 0,
    'ALTER TABLE news ADD COLUMN ai_embedding_attempts int NOT NULL DEFAULT 0 AFTER ai_embedding_model',
    'SELECT 1'
);
PREPARE news_embedding_attempts_stmt FROM @embedding_attempts_sql;
EXECUTE news_embedding_attempts_stmt;
DEALLOCATE PREPARE news_embedding_attempts_stmt;

SET @sentiment_index := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'news'
      AND index_name = 'idx_news_sentiment_status'
);
SET @sentiment_index_sql := IF(
    @sentiment_index = 0,
    'CREATE INDEX idx_news_sentiment_status ON news (ai_sentiment_status, ai_sentiment_attempts)',
    'SELECT 1'
);
PREPARE news_sentiment_index_stmt FROM @sentiment_index_sql;
EXECUTE news_sentiment_index_stmt;
DEALLOCATE PREPARE news_sentiment_index_stmt;

-- =============================================================================
-- V35: 포트폴리오 공유 카드 운영 상태
-- -----------------------------------------------------------------------------
-- 목적
--   - 관리자가 공개 공유 카드를 경고, 블라인드, 삭제로 표시
--   - 삭제된 카드는 목록에 남기되 본문을 볼 수 없고 다시 수정하지 않음
-- 대상
--   - portfolio_share.moderation_status
-- 의존
--   - V33 portfolio_share
-- 연관 API
--   - GET  /api/v1/portfolio/shares
--   - POST /api/v1/portfolio/shares/{shareId}/moderation
-- 주의
--   - 기존 행은 OPEN. PUBLIC 목록 조건은 그대로 두고 상태만 카드에 실어 보낸다
-- =============================================================================

ALTER TABLE portfolio_share
    ADD COLUMN moderation_status VARCHAR(16) NOT NULL DEFAULT 'OPEN'
        COMMENT 'OPEN, WARN, BLIND, REMOVED' AFTER cheer_count;

-- =============================================================================
-- V37: 포트폴리오 공유 신고 사유
-- -----------------------------------------------------------------------------
-- 목적
--   - 상세 화면에서 고른 신고 사유를 저장
--   - 관리자 자동 탐지가 이 사유와 공개 글 판별을 함께 집계
-- 대상
--   - portfolio_share_report.reason
-- 의존
--   - V36 portfolio_share_report
-- 연관 API
--   - POST /api/v1/portfolio/shares/{shareId}/reports
--   - GET  /api/v1/admin/portfolio/detections
-- 주의
--   - 기존 신고 행은 reason 이 NULL. 집계에서는 비어 있는 사유를 세지 않는다
-- =============================================================================

ALTER TABLE portfolio_share_report
    ADD COLUMN reason VARCHAR(40) NULL COMMENT '신고 사유. 오픈채팅 URL, 과장 수익문구, 리딩방 패턴, 투자 사기, 개인정보, 광고' AFTER user_id;

-- =============================================================================
-- V38: 포트폴리오 공유 공개 Snapshot과 댓글
-- -----------------------------------------------------------------------------
-- 목적
--   - 공유 당시 공개하기로 한 금액·구성·월 기록만 JSON으로 고정한다
--   - 공유 상세에서 회원이 짧은 댓글을 남긴다
-- 대상
--   - portfolio_share.public_detail
--   - portfolio_share_comment
-- 의존
--   - V33 portfolio_share
-- 연관 API
--   - GET  /api/v1/portfolio/shares/{shareId}
--   - GET  /api/v1/portfolio/shares/{shareId}/comments
--   - POST /api/v1/portfolio/shares/{shareId}/comments
-- 주의
--   - public_detail에는 계좌번호, 보관처, 주민번호, 전화번호를 넣지 않는다
--   - 기존 행은 public_detail이 비어 있고, 상세는 저장된 공개 문구로 합성한다
--   - 작성자 이름은 상세 응답에서 첫 글자만 보여 준다
--   - 삭제된 공유에는 댓글을 받지 않는다
-- =============================================================================

ALTER TABLE portfolio_share
    ADD COLUMN public_detail TEXT NULL COMMENT '공개 Snapshot JSON. 계좌·보관처는 제외' AFTER message;

-- =============================================================================
-- V39: 포트폴리오 공유 신고 상세 사유
-- -----------------------------------------------------------------------------
-- 목적
--   - 신고 팝업에서 고른 종류와 따로, 회원이 적은 사유 문장을 저장한다
-- 대상
--   - portfolio_share_report.note
-- 의존
--   - V37 portfolio_share_report.reason
-- 연관 API
--   - POST /api/v1/portfolio/shares/{shareId}/reports
-- 주의
--   - 자동 탐지 집계는 reason(종류)만 사용한다. note 는 신고 원문이다
--   - 기존 신고 행은 note 가 NULL 이다
-- =============================================================================

ALTER TABLE portfolio_share_report
    ADD COLUMN note VARCHAR(500) NULL COMMENT '회원이 입력한 신고 사유' AFTER reason;
