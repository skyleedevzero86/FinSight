-- =============================================================================
-- V33: 커뮤니티 포트폴리오 공유
-- -----------------------------------------------------------------------------
-- 목적
--   - 나의 포트폴리오에서 공유하기를 누르면 커뮤니티에 카드로 노출
--   - 설명, 금액 구간, 목표, 이번 달 증감을 카드에 저장
--   - 화면 확인용 더미 카드와 이번 주 인기 목표
-- 대상
--   - portfolio_share
--   - portfolio_share_goal
-- 연관 API
--   - GET  /api/v1/portfolio/shares
--   - POST /api/v1/portfolio/shares
-- 주의
--   - visibility 가 PUBLIC 인 카드만 커뮤니티 목록에 나온다
-- =============================================================================

CREATE TABLE IF NOT EXISTS portfolio_share (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '공유 카드 PK',
    user_id BIGINT NOT NULL COMMENT '공유한 사용자 ID. 더미는 0',
    author_name VARCHAR(50) NOT NULL COMMENT '사용자명',
    message VARCHAR(500) NOT NULL COMMENT '공유 설명',
    visibility VARCHAR(16) NOT NULL COMMENT 'PUBLIC, FOLLOWERS, PRIVATE',
    goal_label VARCHAR(40) NOT NULL COMMENT '목표 이름',
    amount_label VARCHAR(40) NOT NULL COMMENT '카드에 보이는 금액',
    progress_percent INT NOT NULL COMMENT '목표 진행률 0~100',
    month_rate_label VARCHAR(20) NULL COMMENT '이번 달 증감률',
    goal_rate_label VARCHAR(20) NULL COMMENT '목표 달성률',
    show_asset TINYINT(1) NOT NULL DEFAULT 0 COMMENT '자산 칩 표시',
    show_debt TINYINT(1) NOT NULL DEFAULT 0 COMMENT '부채 칩 표시',
    cheer_count INT NOT NULL DEFAULT 0 COMMENT '응원 수',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '공유 시각',
    PRIMARY KEY (id),
    KEY idx_portfolio_share_public (visibility, id DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='커뮤니티에 공개하는 포트폴리오 공유 카드';

CREATE TABLE IF NOT EXISTS portfolio_share_goal (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '인기 목표 PK',
    goal_label VARCHAR(40) NOT NULL COMMENT '목표 이름',
    participant_count INT NOT NULL COMMENT '참여 인원',
    sort_order INT NOT NULL COMMENT '표시 순서',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='이번 주 인기 목표';

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
