-- =============================================================================
-- finsight_data: 기본 데이터와 값 보정
-- -----------------------------------------------------------------------------
-- 목적
--   - 운영 기본 행을 넣고, 예전 값을 현재 규약에 맞춘다
-- 대상
--   - sms_settings, users, mainimg_item, ulink_item
--   - portfolio_share_goal, portfolio_share, news.source_url
-- 주의
--   - sms_settings 는 다시 실행해도 관리자가 바꾼 값을 덮지 않는다
--   - 뉴스 원문 URL 유니크 제약은 중복을 지운 직후에 이 파일에서 추가한다
-- =============================================================================

-- 초기 싱글톤 행 삽입.
-- created_at/updated_at 을 명시해, 기존 DB에 해당 컬럼 DEFAULT 가 없어도 안전하게 동작.
-- ON DUPLICATE KEY UPDATE id=id : 재마이그레이션·재실행 시 관리자가 바꾼 설정을 유지.
INSERT INTO sms_settings (
    id,
    enabled,
    news_alert_enabled,
    otp_enabled,
    account_recovery_enabled,
    system_alert_enabled,
    notification_enabled,
    default_message_type,
    created_at,
    updated_at
) VALUES (
    1,   -- 싱글톤 PK
    0,   -- 마스터 OFF (운영에서 키 확인 후 관리자 화면에서 ON)
    0,   -- 뉴스 알림 자동 SMS OFF
    1,   -- OTP ON
    1,   -- 계정 복구 ON
    0,   -- 시스템 알림 OFF
    0,   -- 일반 알림 OFF
    'SMS',
    CURRENT_TIMESTAMP(3),
    CURRENT_TIMESTAMP(3)
) ON DUPLICATE KEY UPDATE id = id;

-- =============================================================================
-- V19: 데모 계정 system2 → guest 이름 정리
-- -----------------------------------------------------------------------------
-- 목적
--   - users.username / users.nickname 이 'system2' 인 행을 'guest' 로 바꾼다.
--   - 로그인 화면·헤더에 보이던 예전 데모 아이디를 guest 로 통일한다.
--
-- username
--   - system2 → guest
--   - 이미 username='guest' 행이 있으면 UNIQUE 충돌을 피하기 위해
--     guest 가 없을 때만 username 을 변경한다.
--
-- nickname
--   - nickname='system2' 이면 guest 로 변경한다.
--   - username 이 system2 로 남은 경우(이미 guest username 존재)에도
--     표시용 nickname 만 guest 로 맞춘다.
--
-- 연관
--   - AuthenticationService.resolveLoginUser 에서 system2 로그인 입력을
--     guest 로 해석한다.
--   - 프론트 LoginForm 기본 아이디는 guest.
-- =============================================================================

UPDATE users
SET nickname = 'guest'
WHERE nickname = 'system2';

UPDATE users
SET username = 'guest'
WHERE username = 'system2'
  AND NOT EXISTS (
      SELECT 1
      FROM (SELECT id FROM users WHERE username = 'guest') AS existing_guest
  );

UPDATE users
SET nickname = 'guest'
WHERE username = 'system2'
  AND (nickname IS NULL OR nickname = '' OR nickname = 'system2');

-- 기존 행: sort_order 가 0(미설정)이면 등록 순으로 1부터 부여
SET @r := (
    SELECT COALESCE(MAX(t.sort_order), 0)
    FROM (SELECT sort_order FROM mainimg_item WHERE sort_order > 0) t
);
UPDATE mainimg_item
SET sort_order = (@r := @r + 1)
WHERE sort_order = 0
ORDER BY created_at ASC, id ASC;

-- 기존 행: sort_order 가 0(미설정)이면 등록 순으로 1부터 부여
SET @r := (
    SELECT COALESCE(MAX(t.sort_order), 0)
    FROM (SELECT sort_order FROM ulink_item WHERE sort_order > 0) t
);
UPDATE ulink_item
SET sort_order = (@r := @r + 1)
WHERE sort_order = 0
ORDER BY created_at ASC, id ASC;

INSERT INTO portfolio_share_goal (goal_label, participant_count, sort_order) VALUES
    ('1억 만들기', 1284, 1),
    ('부채 0원', 883, 2),
    ('비상금 3천', 642, 3),
    ('내집 마련', 518, 4);

INSERT INTO portfolio_share (
    user_id, author_name, message, visibility, goal_label, amount_label, progress_percent,
    month_rate_label, goal_rate_label, show_asset, show_debt, cheer_count, created_at
) VALUES
    (0, 'studio_kim', '디자인 스튜디오를 운영하면서 2천만원을 모으는 게 제 목표입니다. 생활비와 투자를 나눠 적고 있어요.', 'PUBLIC', '1억 만들기', '2,000만원', 60, '+3.7%', '60%', 1, 1, 254, '2026-10-09 09:10:00.000'),
    (0, 'month_park', '디자이너 생활을 하면서 2천만원, 투자수익보다 부채를 줄이는 게 제 목표입니다.', 'PUBLIC', '1억 만들기', '2,000만원', 60, '+3.7%', '60%', 1, 1, 188, '2026-10-09 08:40:00.000'),
    (0, 'cash_lee', '비상금 3천만원을 먼저 채우고, 남는 금액만 적립식 펀드에 넣기로 했습니다.', 'PUBLIC', '비상금 3천', '1,200만원', 40, '+1.2%', '40%', 1, 0, 96, '2026-10-08 19:20:00.000'),
    (0, 'home_choi', '내집 마련 통장을 따로 두고, 이번 달은 추가 투자보다 저축 비중을 올렸습니다.', 'PUBLIC', '내집 마련', '8,500만원', 28, '+0.8%', '28%', 1, 1, 73, '2026-10-07 21:05:00.000');

-- =============================================================================
-- V40: 뉴스 원문 URL 중복 정리와 유니크 제약
-- -----------------------------------------------------------------------------
-- 목적
--   - 같은 기사 URL이 여러 행으로 쌓이지 않게 한다
--   - 끝 슬래시와 앞뒤 공백을 같은 주소로 맞춘 뒤, 가장 작은 id만 남긴다
-- 대상
--   - news.source_url
--   - news_target_categories.news_id
--   - news_reactions.news_id
--   - news_statistics.news_id
--   - comments.target_id (comment_type = NEWS)
--   - notifications.news_id
-- 연관
--   - NewsRepositoryImpl.saveAllNews
--   - NewsCrawlingTasklet
-- 주의
--   - 유니크 제약은 중복 행을 지운 뒤에만 추가한다
--   - 남기는 행은 같은 source_url 그룹의 최소 id다
--   - Java NewsSourceUrls.normalize 와 같이 앞뒤 공백과 끝 슬래시를 제거한다
-- =============================================================================

UPDATE news
SET source_url = TRIM(TRAILING '/' FROM TRIM(source_url))
WHERE source_url IS NOT NULL;

UPDATE news kept
INNER JOIN (
    SELECT MIN(id) AS keep_id, SUM(view_count) AS view_count
    FROM news
    GROUP BY source_url
    HAVING COUNT(*) > 1
) grouped ON kept.id = grouped.keep_id
SET kept.view_count = grouped.view_count;

CREATE TEMPORARY TABLE news_url_dup AS
SELECT n.id AS drop_id, grouped.keep_id
FROM news n
INNER JOIN (
    SELECT source_url, MIN(id) AS keep_id
    FROM news
    GROUP BY source_url
    HAVING COUNT(*) > 1
) grouped ON grouped.source_url = n.source_url
WHERE n.id <> grouped.keep_id;

DELETE category_row
FROM news_target_categories category_row
INNER JOIN news_url_dup dup ON category_row.news_id = dup.drop_id
INNER JOIN news_target_categories kept
    ON kept.news_id = dup.keep_id
   AND kept.category = category_row.category;

UPDATE news_target_categories category_row
INNER JOIN news_url_dup dup ON category_row.news_id = dup.drop_id
SET category_row.news_id = dup.keep_id;

DELETE reaction_row
FROM news_reactions reaction_row
INNER JOIN news_url_dup dup ON reaction_row.news_id = dup.drop_id
INNER JOIN news_reactions kept
    ON kept.news_id = dup.keep_id
   AND kept.user_email = reaction_row.user_email;

UPDATE news_reactions reaction_row
INNER JOIN news_url_dup dup ON reaction_row.news_id = dup.drop_id
SET reaction_row.news_id = dup.keep_id;

UPDATE news_statistics kept
INNER JOIN news_url_dup dup ON kept.news_id = dup.keep_id
INNER JOIN news_statistics drop_row ON drop_row.news_id = dup.drop_id
SET kept.view_count = kept.view_count + drop_row.view_count,
    kept.like_count = kept.like_count + drop_row.like_count,
    kept.dislike_count = kept.dislike_count + drop_row.dislike_count,
    kept.comment_count = kept.comment_count + drop_row.comment_count;

DELETE statistic_row
FROM news_statistics statistic_row
INNER JOIN news_url_dup dup ON statistic_row.news_id = dup.drop_id
INNER JOIN news_statistics kept ON kept.news_id = dup.keep_id;

UPDATE news_statistics statistic_row
INNER JOIN news_url_dup dup ON statistic_row.news_id = dup.drop_id
SET statistic_row.news_id = dup.keep_id;

UPDATE comments comment_row
INNER JOIN news_url_dup dup ON comment_row.target_id = dup.drop_id
SET comment_row.target_id = dup.keep_id
WHERE comment_row.comment_type = 'NEWS';

UPDATE notifications notification_row
INNER JOIN news_url_dup dup ON notification_row.news_id = dup.drop_id
SET notification_row.news_id = dup.keep_id;

DELETE news_row
FROM news news_row
INNER JOIN news_url_dup dup ON news_row.id = dup.drop_id;

DROP TEMPORARY TABLE news_url_dup;

ALTER TABLE news
    ADD CONSTRAINT uk_news_source_url UNIQUE (source_url);
