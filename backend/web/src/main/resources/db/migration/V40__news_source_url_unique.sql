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
