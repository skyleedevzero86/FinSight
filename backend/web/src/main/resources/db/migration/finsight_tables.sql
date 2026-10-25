-- =============================================================================
-- finsight_tables: 테이블 생성
-- -----------------------------------------------------------------------------
-- 목적
--   - Flyway 가 담당하는 테이블을 만든다
-- 대상
--   - editor_assets, ulink_item, popup_item, mainimg_item
--   - email_verifications, email_logs, email_templates, welcome_email_jobs
--   - live_vod_favorites, live_vod_comments, live_vod_reactions, live_vod_comment_reactions
--   - board_moderation_run, inbox_notification, inbox_settings
--   - sms_settings, sms_send_log, news_reactions
--   - portfolio_asset, portfolio_snapshot, portfolio_share, portfolio_share_goal
--   - portfolio_share_reaction, portfolio_share_report, portfolio_share_comment
--   - news_crawl_incident, news_crawl_attempt, news_crawl_provider_lock
-- 주의
--   - live_vod_ratings 는 만들지 않고, 예전 환경에 남아 있으면 지운다
--   - email_logs 는 예전 스텁이 있을 수 있어 지운 뒤 다시 만든다
--   - 컬럼·인덱스 추가와 값 보정은 다음 파일에 있다
-- =============================================================================

-- =============================================================================
-- Flyway V1: editor_assets
-- -----------------------------------------------------------------------------
-- 에디터 에서 업로드한 이미지·파일의 메타데이터를 저장한다.
-- 실제 바이너리는 객체 스토리지에 두고, 여기서는 id·object_key·
-- bucket·원본 파일명·MIME·크기·업로드 시각만 관리한다.
-- object_key 는 스토리지 내 경로/키이며, uk 로 중복 업로드 충돌을 방지한다.
-- uploaded_at 인덱스는 정리·목록 조회 시 시간 범위 필터에 사용한다.
-- =============================================================================

CREATE TABLE IF NOT EXISTS editor_assets (
    id CHAR(36) NOT NULL COMMENT '자산 UUID (클라이언트 또는 서버 생성)',
    object_key VARCHAR(512) NOT NULL COMMENT '버킷 내 객체 키(경로 포함)',
    bucket_name VARCHAR(80) NOT NULL COMMENT '저장소 버킷 이름',
    original_file_name VARCHAR(255) NOT NULL COMMENT '사용자가 올린 원본 파일명',
    content_type VARCHAR(160) NOT NULL COMMENT 'MIME 타입 (예: image/png)',
    file_size BIGINT NOT NULL COMMENT '파일 크기(바이트)',
    uploaded_at DATETIME(3) NOT NULL COMMENT '업로드 완료 시각(밀리초)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_editor_assets_object_key (object_key(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='에디터 업로드 자산 메타데이터 (바이너리는 외부 스토리지)';

-- =============================================================================
-- Flyway V2: ulink_item 
-- domain_id: 멀티 사이트/테넌트 구분용 선택 필드
-- =============================================================================

CREATE TABLE IF NOT EXISTS ulink_item (
    id VARCHAR(32) NOT NULL COMMENT '업무 키(예: ULK 접두 + 타임스탬프)',
    domain_id VARCHAR(32) COMMENT '사이트·테넌트 구분',
    section_code VARCHAR(32) COMMENT '통합링크 구분(메뉴/푸터 등)',
    link_group VARCHAR(100) COMMENT '같은 구역 내 그룹명',
    link_name VARCHAR(200) NOT NULL COMMENT '링크 표시명',
    link_url VARCHAR(500) NOT NULL COMMENT '이동 URL',
    link_target VARCHAR(32) COMMENT '링크 타겟',
    description VARCHAR(1000) COMMENT '비고·설명',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '등록 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',
    PRIMARY KEY (id),
    KEY idx_ulink_domain (domain_id),
    KEY idx_ulink_section (section_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='통합링크 항목';

-- =============================================================================
-- Flyway V3: 팝업, 메인이미지
-- -----------------------------------------------------------------------------
-- domain_id: 멀티 사이트·테넌트 구분
-- =============================================================================
-- popup_item: 레이어 팝업
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS popup_item (
    id VARCHAR(32) NOT NULL COMMENT '업무 키(예: POP + 타임스탬프)',
    domain_id VARCHAR(32) COMMENT '사이트·테넌트 구분',
    title VARCHAR(200) NOT NULL COMMENT '팝업 제목',
    file_url VARCHAR(500) COMMENT '첨부·연결 파일 URL',
    link_target VARCHAR(32) COMMENT '링크 타겟(_blank 등)',
    img_path VARCHAR(500) COMMENT '이미지 경로 또는 URL',
    file_name VARCHAR(200) COMMENT '표시용 파일명',
    vertical_pos INT COMMENT '세로 위치(픽셀 등, 화면 규약)',
    width_pos INT COMMENT '가로 위치',
    vertical_size INT COMMENT '세로 크기',
    width_size INT COMMENT '가로 크기',
    notice_begin VARCHAR(20) COMMENT '노출 시작(문자열, 운영 규약)',
    notice_end VARCHAR(20) COMMENT '노출 종료',
    stop_today_hide CHAR(1) NOT NULL DEFAULT 'N' COMMENT 'Y: 오늘 하루 보지 않기 적용',
    notice_active CHAR(1) NOT NULL DEFAULT 'Y' COMMENT 'Y/N 게시(노출) 활성',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '등록 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',
    PRIMARY KEY (id),
    KEY idx_popup_domain (domain_id),
    KEY idx_popup_notice (notice_active, notice_begin, notice_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='레이어 팝업 항목';

-- -----------------------------------------------------------------------------
-- mainimg_item: 메인 비주얼·히어로 이미지 슬롯
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mainimg_item (
    id VARCHAR(32) NOT NULL COMMENT '업무 키(예: IMG + 타임스탬프)',
    domain_id VARCHAR(32) COMMENT '사이트·테넌트 구분',
    image_name VARCHAR(200) NOT NULL COMMENT '슬롯·이미지 표시명',
    image VARCHAR(500) COMMENT '이미지 URL 또는 경로',
    image_file VARCHAR(500) COMMENT '업로드 파일 경로·식별자',
    description VARCHAR(1000) COMMENT '부가 설명',
    reflect_yn CHAR(1) NOT NULL DEFAULT 'Y' COMMENT 'Y/N 화면 반영(노출) 여부',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '등록 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',
    PRIMARY KEY (id),
    KEY idx_mainimg_domain (domain_id),
    KEY idx_mainimg_reflect (reflect_yn)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='메인 이미지(히어로) 항목';

-- =============================================================================
-- V7: 이메일 인증 챌린지
-- -----------------------------------------------------------------------------
-- 목적
--   - 대상 테이블명 email_verifications
--   - 회원가입·아이디찾기·비밀번호찾기 등 용도의 이메일 OTP 인증을 저장한다.
--   - 인증코드는 code_hash 로만 보관하고, 평문 코드는 검증 과정에서만 잠시 기록한다.
--
-- 주요 흐름
--   1. 요청 → challenge_id + code_hash + expires_at 저장. status 는 PENDING 등
--   2. 코드 확인 → verified_at 기록
--   3. 실제 가입·재설정 완료 → consumed_at 기록. 재사용 방지
--
-- purpose 예
--   - VERIFICATION_SIGNUP
--   - VERIFICATION_FIND_EMAIL
--   - VERIFICATION_FIND_PASSWORD
-- =============================================================================

CREATE TABLE IF NOT EXISTS email_verifications (
    -- 기본키
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '이메일 인증 챌린지 PK',

    -- 용도
    purpose VARCHAR(32) NOT NULL COMMENT '인증 용도 코드 (회원가입/아이디찾기/비밀번호찾기 등)',
    purpose_label VARCHAR(50) NOT NULL COMMENT '인증 용도 표시명',

    -- 대상 / 챌린지
    email VARCHAR(255) NOT NULL COMMENT '인증 대상 이메일',
    challenge_id VARCHAR(36) NOT NULL COMMENT '챌린지 토큰(UUID). 프론트 verify URL 경로에 사용',
    code_hash VARCHAR(100) NOT NULL COMMENT '인증코드 해시 (평문 저장 금지)',
    verified_code VARCHAR(6) NULL COMMENT '검증에 성공한 코드(감사/디버그용, 선택)',
    last_entered_code VARCHAR(6) NULL COMMENT '마지막 입력 코드(실패 추적)',

    -- 요청 메타
    request_ip VARCHAR(64) NULL COMMENT '인증 요청 IP',
    request_location VARCHAR(255) NULL COMMENT 'IP 기반 대략 위치',
    requested_at DATETIME(6) NOT NULL COMMENT '인증 요청 시각',
    expires_at DATETIME(6) NOT NULL COMMENT '코드 만료 시각',
    verified_at DATETIME(6) NULL COMMENT '코드 검증 성공 시각',
    consumed_at DATETIME(6) NULL COMMENT '가입/재설정 등 최종 사용 시각',
    attempt_count INT NOT NULL DEFAULT 0 COMMENT '코드 입력 시도 횟수',
    status VARCHAR(20) NOT NULL COMMENT '챌린지 상태 (PENDING/VERIFIED/EXPIRED/CONSUMED 등)',

    -- 감사 시각
    created_at DATETIME(6) NOT NULL COMMENT '레코드 생성 시각',
    updated_at DATETIME(6) NOT NULL COMMENT '레코드 수정 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uk_email_verifications_challenge (challenge_id),
    KEY idx_email_verifications_email_purpose (email, purpose, status),
    KEY idx_email_verifications_requested_at (requested_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='이메일 OTP 인증 챌린지';

-- =============================================================================
-- V9: 메일 발송 이력
-- -----------------------------------------------------------------------------
-- 목적
--   - 대상 테이블명 email_logs
--   - 시스템·사용자·비로그인 요청으로 나간 모든 메일의 발송 결과를 보관한다.
--   - 관리자가 언제·누구에게·어떤 용도로·어디서 요청했는지 IP 등으로 추적할 수 있게 한다.
--
-- 비로그인 정책
--   - actor_type = 'ANONYMOUS' 인 경우 user_id 는 NULL 일 수 있다.
--   - 대신 request_ip / request_location / user_agent 로 요청자를 식별·관리한다.
--
-- 마이그레이션 주의
--   - 이전 EmailLog 스텁이 ddl-auto 로 부분 생성됐을 수 있어 테이블을 재생성한다.
--   - 당시에는 실제 발송 이력으로 쓰이지 않았으므로 DROP 후 CREATE 한다.
-- =============================================================================

DROP TABLE IF EXISTS email_logs;

CREATE TABLE email_logs (
    -- 기본키
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '메일 발송 이력 PK',

    -- 메일 헤더/본문 요약
    recipient VARCHAR(255) NOT NULL COMMENT '수신 이메일 주소',
    subject VARCHAR(500) NOT NULL COMMENT '메일 제목',
    template_type VARCHAR(64) NULL COMMENT '템플릿/발송 유형 코드 (예: verification-code)',
    purpose VARCHAR(64) NOT NULL COMMENT '발송 용도 enum (VERIFICATION_SIGNUP, NEWS_ALERT 등)',
    purpose_label VARCHAR(100) NOT NULL COMMENT '발송 용도 한글 라벨',
    status VARCHAR(20) NOT NULL COMMENT '발송 상태 (SENT, FAILED 등)',
    from_address VARCHAR(255) NULL COMMENT '발신 이메일 주소(SMTP username)',

    -- 사용자/요청 주체
    user_id BIGINT NULL COMMENT '관련 사용자 ID (비로그인이면 NULL 가능)',
    actor_type VARCHAR(20) NOT NULL COMMENT '요청 주체 (ANONYMOUS, USER, SYSTEM, ADMIN)',
    actor_user_id BIGINT NULL COMMENT '실제 조작한 사용자 ID(관리자/본인 등)',

    -- 비로그인·보안 추적
    request_ip VARCHAR(64) NULL COMMENT '요청 클라이언트 IP (비로그인 추적용)',
    request_location VARCHAR(255) NULL COMMENT 'IP 기반 대략 위치 문자열',
    user_agent VARCHAR(512) NULL COMMENT '요청 User-Agent',

    -- 본문/오류. 민감정보는 마스킹된 미리보기만 저장
    body_preview TEXT NULL COMMENT '본문 미리보기(HTML 제거, OTP/인증코드 마스킹)',
    error_message TEXT NULL COMMENT '발송 실패 시 오류 메시지',
    related_ref VARCHAR(100) NULL COMMENT '연관 참조키 (예: email verification challengeId)',

    -- 전달/열람 확장 필드. 현재는 SMTP 단방향이라 대부분 NULL
    sent_at DATETIME(6) NULL COMMENT 'SMTP 전송 성공 시각',
    delivered_at DATETIME(6) NULL COMMENT '수신 서버 전달 시각(확장용)',
    opened_at DATETIME(6) NULL COMMENT '열람 시각(확장용)',
    clicked_at DATETIME(6) NULL COMMENT '링크 클릭 시각(확장용)',
    bounced_at DATETIME(6) NULL COMMENT '반송 시각(확장용)',
    bounce_reason VARCHAR(500) NULL COMMENT '반송 사유(확장용)',

    -- 감사 시각
    created_at DATETIME(6) NOT NULL COMMENT '이력 생성 시각',
    updated_at DATETIME(6) NULL COMMENT '이력 수정 시각',

    PRIMARY KEY (id),

    -- 관리자 목록/필터용 인덱스
    KEY idx_email_logs_created_at (created_at),
    KEY idx_email_logs_recipient (recipient),
    KEY idx_email_logs_request_ip (request_ip),
    KEY idx_email_logs_status (status),
    KEY idx_email_logs_purpose (purpose),
    KEY idx_email_logs_user_id (user_id),
    KEY idx_email_logs_actor_type (actor_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='메일 발송 이력';

-- =============================================================================
-- V10: 이메일 HTML 템플릿 저장소
-- -----------------------------------------------------------------------------
-- 목적
--   - 검증 코드·환영 메일 등 HTML 템플릿을 DB에서 관리한다.
--   - 대상 테이블명 email_templates
--   - 애플리케이션 기동 시 EmailTemplateSeedRunner 가 classpath HTML로
--     name 기준 시드 또는 갱신을 한다.
--
-- 사용 방식
--   - name : 템플릿 식별키. 예 verification-code, welcome
--   - subject / html_content : {{변수}} 플레이스홀더 치환
--   - template_variables : 사용 가능한 변수 목록. JSON 문자열
--   - active = 0 이면 발송 시 해당 템플릿을 건너뛰고 classpath 폴백을 쓴다.
-- =============================================================================

CREATE TABLE IF NOT EXISTS email_templates (
    -- 기본키
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '이메일 템플릿 PK',

    -- 템플릿 정의
    name VARCHAR(255) NOT NULL COMMENT '템플릿 고유 이름 (예: verification-code, welcome)',
    subject VARCHAR(500) NOT NULL COMMENT '메일 제목 템플릿 ({{appName}} 등 변수 가능)',
    html_content LONGTEXT NULL COMMENT 'HTML 본문 템플릿',
    text_content TEXT NULL COMMENT '텍스트 본문 템플릿(선택)',
    template_variables JSON NULL COMMENT '템플릿 변수 목록 JSON (문서/관리용)',
    active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1=사용, 0=비활성(발송 시 폴백)',

    -- 감사 시각
    created_at DATETIME(6) NOT NULL COMMENT '템플릿 생성 시각',
    updated_at DATETIME(6) NULL COMMENT '템플릿 수정 시각',

    PRIMARY KEY (id),
    -- name 으로 단건 조회·시드 갱신
    UNIQUE KEY uk_email_templates_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='이메일 HTML 템플릿 저장소';

-- =============================================================================
-- V11: 회원가입 축하 메일 발송 큐
-- -----------------------------------------------------------------------------
-- 목적
--   - 회원가입 직후 축하 메일을 즉시가 아니라 가입 시각 기준 2시간 이내에
--     안정적으로 발송하기 위한 작업 큐다.
--   - 대상 테이블명 welcome_email_jobs
--   - 서버 재시작 후에도 PENDING 작업을 DB에서 다시 읽어 발송할 수 있다.
--
-- 발송 정책
--   - registered_at : 회원가입 또는 예약 생성 시각
--   - deadline_at   : registered_at + 2시간. 이 시각을 넘기면 EXPIRED
--   - scheduled_at  : 실제 발송 시도 예정 시각. 기본은 가입 직후 약 30초
--   - 스케줄러가 status=PENDING 이고 scheduled_at <= now <= deadline_at 인 행을 처리한다.
--
-- status 값
--   - PENDING : 발송 대기
--   - SENT    : 발송 성공
--   - EXPIRED : 2시간 기한 초과로 미발송 종료
--   - FAILED  : 재시도 한도 초과 또는 최종 실패
--
-- 제약
--   - user_id UNIQUE : 사용자당 축하 메일 작업은 1건만 유지한다.
-- =============================================================================

CREATE TABLE IF NOT EXISTS welcome_email_jobs (
    -- 기본키
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '축하 메일 작업 PK',

    -- 대상 사용자 / 시간 윈도우
    user_id BIGINT NOT NULL COMMENT '대상 사용자 ID (users.id)',
    registered_at DATETIME(6) NOT NULL COMMENT '회원가입 시각 (발송 윈도우 기준점)',
    deadline_at DATETIME(6) NOT NULL COMMENT '발송 마감 시각 (registered_at + 2시간)',
    scheduled_at DATETIME(6) NOT NULL COMMENT '다음 발송 시도 예정 시각',

    -- 처리 상태
    status VARCHAR(20) NOT NULL COMMENT '작업 상태 (PENDING|SENT|EXPIRED|FAILED)',
    attempt_count INT NOT NULL DEFAULT 0 COMMENT '발송 시도 횟수',
    last_error TEXT NULL COMMENT '마지막 실패 사유',
    sent_at DATETIME(6) NULL COMMENT '발송 성공 시각',

    -- 감사 시각
    created_at DATETIME(6) NOT NULL COMMENT '작업 생성 시각',
    updated_at DATETIME(6) NULL COMMENT '작업 수정 시각',

    PRIMARY KEY (id),

    -- 사용자당 1건만 허용
    UNIQUE KEY uk_welcome_email_jobs_user (user_id),
    -- 스케줄러: 대기열 조회
    KEY idx_welcome_email_jobs_pending (status, scheduled_at),
    -- 스케줄러: 기한 만료 정리
    KEY idx_welcome_email_jobs_deadline (status, deadline_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회원가입 축하 메일 발송 큐. 가입 후 2시간 이내';

-- =============================================================================
-- V12: LIVE/VOD 참여(즐겨찾기·댓글)
-- -----------------------------------------------------------------------------
-- 목적
--   - LIVE/VOD 영상(video_id) 단위로 사용자 즐겨찾기와 댓글/대댓글을 저장한다.
--   - 목록 썸네일에는 즐겨찾기 수·댓글 수를 표시하고,
--     상세 화면에서는 즐겨찾기 토글과 댓글/답글을 제공한다.
--
-- 대상 테이블
--   - live_vod_favorites : 사용자별 즐겨찾기 (user_email + video_id 유일)
--   - live_vod_comments  : 댓글·대댓글 (parent_id 로 1단계 답글)
--
-- 참여 정책
--   - video_id        : YouTube 영상 ID 문자열
--   - user_email      : 로그인 사용자 식별 (즐겨찾기·댓글 작성자)
--   - parent_id NULL  : 최상위 댓글
--   - parent_id 값    : 해당 댓글에 대한 1단계 대댓글
--   - 대댓글에 다시 답글은 애플리케이션에서 제한한다.
--
-- 제약
--   - 즐겨찾기는 (user_email, video_id) UNIQUE 로 사용자당 영상 1건만 유지한다.
--   - 댓글 parent_id 는 자기 참조 FK 이며, 부모 삭제 시 대댓글도 CASCADE 삭제한다.
-- =============================================================================

CREATE TABLE IF NOT EXISTS live_vod_favorites (
    -- 기본키
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '즐겨찾기 PK',

    -- 대상 영상 / 사용자
    video_id VARCHAR(32) NOT NULL COMMENT 'YouTube videoId',
    user_email VARCHAR(255) NOT NULL COMMENT '즐겨찾기한 사용자 이메일',

    -- 감사 시각
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '즐겨찾기 등록 시각',

    PRIMARY KEY (id),

    -- 사용자당 영상 1건만 허용
    UNIQUE KEY uk_live_vod_fav_user_video (user_email, video_id),
    -- 목록 집계: 영상별 즐겨찾기 수
    KEY idx_live_vod_fav_video (video_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LIVE/VOD 즐겨찾기. 사용자·영상당 1건';

CREATE TABLE IF NOT EXISTS live_vod_comments (
    -- 기본키
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '댓글 PK',

    -- 대상 영상 / 작성자
    video_id VARCHAR(32) NOT NULL COMMENT 'YouTube videoId',
    user_email VARCHAR(255) NOT NULL COMMENT '작성자 이메일',
    author_nickname VARCHAR(100) NULL COMMENT '작성자 닉네임(표시용)',

    -- 본문 / 대댓글
    content TEXT NOT NULL COMMENT '댓글 본문',
    parent_id BIGINT NULL COMMENT '부모 댓글 ID (NULL이면 최상위 댓글)',

    -- 감사 시각
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '작성 시각',
    updated_at DATETIME(6) NULL COMMENT '수정 시각',

    PRIMARY KEY (id),

    -- 영상별 댓글 목록/집계
    KEY idx_live_vod_cmt_video (video_id, created_at),
    -- 대댓글 조회
    KEY idx_live_vod_cmt_parent (parent_id),

    -- 부모 댓글 삭제 시 대댓글도 함께 삭제
    CONSTRAINT fk_live_vod_cmt_parent
        FOREIGN KEY (parent_id) REFERENCES live_vod_comments (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LIVE/VOD 댓글·대댓글. parent_id로 1단계 답글';

-- =============================================================================
-- V13: LIVE/VOD 별점(live_vod_ratings) 테이블 제거
-- -----------------------------------------------------------------------------
-- 목적
--   - 별점 기능을 철회하고, 참여 기능을 즐겨찾기·댓글만 유지한다.
--   - V12 초기 버전에서 live_vod_ratings 가 생성된 DB에도 안전하게 적용한다.
--   - 신규 환경에서는 V12 에 별점 테이블이 없으므로 DROP IF EXISTS 만 수행한다.
--
-- 영향
--   - live_vod_ratings 테이블 및 관련 데이터 삭제
--   - live_vod_favorites / live_vod_comments 는 유지
-- =============================================================================

DROP TABLE IF EXISTS live_vod_ratings;

-- =============================================================================
-- V14: LIVE/VOD 영상 좋아요/싫어요
-- -----------------------------------------------------------------------------
-- 사용자당 영상 1개의 반응만 유지 (LIKE | DISLIKE). 같은 버튼 재클릭 시 해제.
-- =============================================================================

CREATE TABLE IF NOT EXISTS live_vod_reactions (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '반응 PK',
    video_id VARCHAR(32) NOT NULL COMMENT 'YouTube videoId',
    user_email VARCHAR(255) NOT NULL COMMENT '반응한 사용자 이메일',
    reaction_type VARCHAR(16) NOT NULL COMMENT 'LIKE 또는 DISLIKE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 시각',
    updated_at DATETIME(6) NULL COMMENT '변경 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uk_live_vod_rxn_user_video (user_email, video_id),
    KEY idx_live_vod_rxn_video_type (video_id, reaction_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LIVE/VOD 영상 좋아요·싫어요';

-- =============================================================================
-- V15: LIVE/VOD 댓글·대댓글 좋아요/싫어요
-- -----------------------------------------------------------------------------
-- 댓글(comment_id) 단위로 사용자당 1개 반응(LIKE|DISLIKE)만 유지한다.
-- =============================================================================

CREATE TABLE IF NOT EXISTS live_vod_comment_reactions (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '댓글 반응 PK',
    comment_id BIGINT NOT NULL COMMENT 'live_vod_comments.id',
    user_email VARCHAR(255) NOT NULL COMMENT '반응한 사용자 이메일',
    reaction_type VARCHAR(16) NOT NULL COMMENT 'LIKE 또는 DISLIKE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 시각',
    updated_at DATETIME(6) NULL COMMENT '변경 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uk_live_vod_cmt_rxn_user (user_email, comment_id),
    KEY idx_live_vod_cmt_rxn_comment_type (comment_id, reaction_type),

    CONSTRAINT fk_live_vod_cmt_rxn_comment
        FOREIGN KEY (comment_id) REFERENCES live_vod_comments (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LIVE/VOD 댓글·대댓글 좋아요·싫어요';

-- =============================================================================
-- V16: 게시판 신고 과다 숨김 실행 이력
-- -----------------------------------------------------------------------------
-- 목적
--   - 대상 테이블명 board_moderation_run
--   - 관리자 수동 실행 또는 배치가 신고 임계값 이상 ACTIVE 글을 HIDDEN으로
--     일괄 숨김 처리할 때, 실행 메타·숨김 대상 상세를 남겨 추적한다.
--   - 관리자 UI(/admin/moderation)의 「실행 이력」「실행 상세」조회에 사용한다.
--
-- 대상 범위
--   - 커뮤니티 게시 유형만: NOTICE, FREE, QNA, COMMUNITY
--   - MEDIA(유튜브·에디터 연동 글)는 일괄 숨김·이력 대상에서 제외한다.
--
-- triggered_by
--   - MANUAL : 관리자가 API/화면에서 즉시 실행 (actor_email 기록)
--   - BATCH  : boardModerationJob 스케줄 실행 (actor_email 은 NULL 가능)
--
-- details_json
--   - 숨김 처리된 글 목록 JSON 배열
--   - 예: [{"boardId":1,"title":"...","authorEmail":"...","boardType":"FREE","reportCount":5}, ...]
--
-- 연관 API
--   - POST /api/v1/admin/boards/maintenance/hide-over-reported
--   - GET  /api/v1/admin/boards/maintenance/runs
--   - GET  /api/v1/admin/boards/maintenance/runs/{runId}
-- =============================================================================

CREATE TABLE IF NOT EXISTS board_moderation_run (
    -- 기본키
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '모더레이션 실행 이력 PK',

    -- 실행 주체
    triggered_by VARCHAR(32) NOT NULL COMMENT '실행 경로 (MANUAL: 관리자 수동, BATCH: 스케줄 배치)',
    actor_email VARCHAR(255) NULL COMMENT '수동 실행 관리자 이메일 (BATCH면 NULL 가능)',

    -- 실행 조건·결과 요약
    report_threshold INT NOT NULL COMMENT '적용한 신고 건수 임계값 (이상이면 숨김 대상)',
    hidden_count INT NOT NULL DEFAULT 0 COMMENT '이번 실행에서 HIDDEN으로 바꾼 게시글 건수',

    -- 상세 스냅샷
    details_json MEDIUMTEXT NULL COMMENT '숨김 대상 상세 JSON (boardId, title, authorEmail, boardType, reportCount)',

    -- 시각
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '실행(이력 저장) 시각',

    PRIMARY KEY (id),
    KEY idx_moderation_run_created (created_at),
    KEY idx_moderation_run_triggered (triggered_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='게시판 신고 과다 숨김 실행 이력 (수동·배치)';

-- =============================================================================
-- V17: 인앱 알림함 (inbox)
-- -----------------------------------------------------------------------------
-- 목적
--   - 사용자별 인앱 알림 목록·읽음·삭제·미읽음 카운트
--   - 카테고리: YOUTUBE, NEWS, COMMENT, QNA, WATCHLIST, ADMIN
--   - 사용자 알림 수신 설정(유튜브/뉴스/댓글/QnA) 저장
--
-- 연관 API
--   - GET/DELETE /api/v1/inbox
--   - GET /api/v1/inbox/unread-count
--   - POST /api/v1/inbox/{id}/read, /api/v1/inbox/read-all
--   - GET/PUT /api/v1/inbox/settings
--   - POST /api/v1/admin/inbox/broadcast
-- =============================================================================

CREATE TABLE IF NOT EXISTS inbox_notification (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '인앱 알림 PK',
    recipient_user_id BIGINT NOT NULL COMMENT '수신 사용자 ID',
    category VARCHAR(32) NOT NULL COMMENT '카테고리 (YOUTUBE|NEWS|COMMENT|QNA|WATCHLIST|ADMIN)',
    actor_user_id BIGINT NULL COMMENT '행위자 사용자 ID (시스템이면 NULL)',
    actor_name VARCHAR(100) NULL COMMENT '행위자 표시명',
    actor_avatar_url VARCHAR(500) NULL COMMENT '행위자 아바타 URL',
    title VARCHAR(500) NOT NULL COMMENT '알림 제목/본문 요약',
    body VARCHAR(1000) NULL COMMENT '부가 스니펫(댓글 미리보기 등)',
    link_url VARCHAR(500) NULL COMMENT '클릭 시 이동 경로',
    ref_type VARCHAR(32) NULL COMMENT '연관 리소스 타입 (BOARD|NEWS|COMMENT|MEDIA 등)',
    ref_id BIGINT NULL COMMENT '연관 리소스 ID',
    is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT '읽음 여부 (0:미읽음, 1:읽음)',
    read_at DATETIME(3) NULL COMMENT '읽은 시각',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '소프트 삭제 (0:활성, 1:삭제)',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',

    PRIMARY KEY (id),
    KEY idx_inbox_recipient_created (recipient_user_id, deleted, created_at DESC, id DESC),
    KEY idx_inbox_recipient_unread (recipient_user_id, deleted, is_read, created_at DESC),
    KEY idx_inbox_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='사용자 인앱 알림함';

CREATE TABLE IF NOT EXISTS inbox_settings (
    user_id BIGINT NOT NULL COMMENT '사용자 ID (PK)',
    youtube_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '유튜브(미디어) 알림 수신',
    news_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '뉴스 알림 수신',
    comment_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '댓글 알림 수신',
    qna_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'QnA 알림 수신',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',

    PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='인앱 알림 카테고리 수신 설정';

-- =============================================================================
-- V18: SMS 발송 설정·이력 (Solapi)
-- -----------------------------------------------------------------------------
-- 목적
--   - 관리자 SMS 마스터/용도별 자동 발송 on·off 설정 (sms_settings, 싱글톤 1행)
--   - Solapi SMS/LMS/MMS 발송·스킵·실패 이력 및 통계용 로그 (sms_send_log)
--   - OTP 등 민감 본문은 애플리케이션에서 content_preview 를 [REDACTED] 로 저장
--
-- sms_settings
--   - id=1 고정 싱글톤. 재실행 시 ON DUPLICATE KEY 로 기존 설정을 덮어쓰지 않음
--   - enabled=0 이면 용도별 스위치와 무관하게 자동 발송 스킵
--   - default_from_number 가 비어 있으면 설정값 solapi.default-from-number 사용
--
-- sms_send_log.status
--   - SENT    : Solapi(또는 시뮬레이션) 발송 성공
--   - FAILED  : 발송 실패 (error_message 에 사유)
--   - SKIPPED : 관리자 설정 비활성 등으로으로 의도적 미발송
--
-- sms_send_log.purpose
--   - NEWS_ALERT | OTP | ACCOUNT_RECOVERY | SYSTEM | NOTIFICATION | MANUAL
--
-- sms_send_log.message_type
--   - SMS | LMS | MMS | KAKAO_ALIMTALK | KAKAO_FRIENDTALK 등
--   - 관리자 수동 발송에서 카카오 타입은 API 단에서 거부 (UNSUPPORTED)
--
-- 연관 API
--   - GET/PUT  /api/v1/admin/sms/settings
--   - POST     /api/v1/admin/sms/send
--   - GET      /api/v1/admin/sms/balance
--   - POST     /api/v1/admin/sms/upload-image
--   - GET      /api/v1/admin/sms/logs   (status·purpose 단독/복합 필터)
--   - GET      /api/v1/admin/sms/stats
--   - (레거시) /api/notification/sms  → 관리자 권한 + ApiResponse
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1) SMS 발송 설정 (싱글톤)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sms_settings (
    -- 기본키 (항상 id=1 한 행)
    id BIGINT NOT NULL COMMENT '설정 PK (싱글톤 1행, 고정값 1)',

    -- 마스터·용도별 스위치 (0:꺼짐, 1:켜짐)
    enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'SMS 마스터 스위치 (0이면 전체 자동발송 스킵)',
    news_alert_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '뉴스 알림 자동 SMS 허용',
    otp_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'OTP SMS 발송 허용',
    account_recovery_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '계정 복구 SMS 발송 허용',
    system_alert_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '시스템 알림 SMS 발송 허용',
    notification_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '일반 알림 SMS 발송 허용',

    -- 발송 기본값
    default_message_type VARCHAR(16) NOT NULL DEFAULT 'SMS'
        COMMENT '기본 메시지 타입 (SMS|LMS|MMS)',
    default_from_number VARCHAR(32) NULL
        COMMENT '기본 발신번호 (NULL/빈값이면 solapi.default-from-number)',

    -- 감사 시각
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',

    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='SMS 발송 설정 (관리자 싱글톤)';

-- -----------------------------------------------------------------------------
-- 2) SMS 발송 이력 (통계·관리자 로그 조회)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sms_send_log (
    -- 기본키
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'SMS 발송 이력 PK',

    -- 발송 분류
    purpose VARCHAR(32) NOT NULL
        COMMENT '용도 (NEWS_ALERT|OTP|ACCOUNT_RECOVERY|SYSTEM|NOTIFICATION|MANUAL)',
    message_type VARCHAR(16) NOT NULL
        COMMENT '메시지 타입 (SMS|LMS|MMS|KAKAO_ALIMTALK|KAKAO_FRIENDTALK 등)',

    -- 수신·발신
    to_phone VARCHAR(32) NOT NULL COMMENT '수신 전화번호',
    from_phone VARCHAR(32) NULL COMMENT '발신 전화번호',

    -- 본문·결과 (OTP purpose 는 앱에서 [REDACTED] 저장·응답)
    content_preview VARCHAR(500) NULL COMMENT '본문 미리보기 (OTP는 [REDACTED])',
    status VARCHAR(16) NOT NULL COMMENT '발송 결과 (SENT|FAILED|SKIPPED)',
    external_message_id VARCHAR(100) NULL COMMENT 'Solapi(외부) 메시지 ID',
    error_message VARCHAR(1000) NULL COMMENT '실패 또는 스킵 사유',

    -- 연관 사용자
    recipient_user_id BIGINT NULL COMMENT '수신 사용자 ID (회원 매핑 시)',
    actor_user_id BIGINT NULL COMMENT '발송 요청자(관리자) 사용자 ID (수동 발송 시)',

    -- 시각
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '발송(기록) 시각',

    PRIMARY KEY (id),
    -- 관리자 로그: 최신순·상태·용도·수신번호 필터
    KEY idx_sms_log_created (created_at),
    KEY idx_sms_log_status (status),
    KEY idx_sms_log_purpose (purpose),
    KEY idx_sms_log_to_phone (to_phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='SMS 발송 이력 (성공·실패·스킵)';

-- =============================================================================
-- V27: 뉴스 좋아요/싫어요
-- -----------------------------------------------------------------------------
-- 목적
--   - 뉴스 상세의 댓글 수 옆에서 기사 단위 좋아요·싫어요를 저장한다.
--   - 사용자당 뉴스 1개만 반응을 유지한다. 같은 버튼을 다시 누르면 해제한다.
--
-- 대상
--   - MySQL finsight.news_reactions
--
-- 연관
--   - GET  /api/v1/news/{newsId}/reactions
--   - POST /api/v1/news/{newsId}/reactions
--   - news_statistics.like_count, dislike_count
--
-- 주의
--   - 댓글 작성은 기존 comments 를 그대로 쓴다.
--   - 반응 변경은 로그인 사용자만 가능하다.
-- =============================================================================

CREATE TABLE IF NOT EXISTS news_reactions (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '반응 PK',
    news_id BIGINT NOT NULL COMMENT '뉴스 ID',
    user_email VARCHAR(255) NOT NULL COMMENT '반응한 사용자 이메일',
    reaction_type VARCHAR(16) NOT NULL COMMENT 'LIKE 또는 DISLIKE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '등록 시각',
    updated_at DATETIME(6) NULL COMMENT '변경 시각',

    PRIMARY KEY (id),
    UNIQUE KEY uk_news_rxn_user_news (user_email, news_id),
    KEY idx_news_rxn_news_type (news_id, reaction_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='뉴스 좋아요·싫어요';

-- =============================================================================
-- V30: 나의 포트폴리오 자산·월별 기록
-- -----------------------------------------------------------------------------
-- 목적
--   - 로그인 사용자별 자산/부채 등록
--   - 월별 순자산 스냅샷 (같은 달은 한 건, 다시 기록하면 갱신)
--
-- 대상
--   - portfolio_asset
--   - portfolio_snapshot
--
-- 연관 API
--   - GET  /api/v1/portfolio
--   - POST /api/v1/portfolio/assets
--   - DELETE /api/v1/portfolio/assets/{assetId}
--   - POST /api/v1/portfolio/snapshots
-- =============================================================================

CREATE TABLE IF NOT EXISTS portfolio_asset (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '자산 PK',
    user_id BIGINT NOT NULL COMMENT '소유 사용자 ID',
    asset_name VARCHAR(80) NOT NULL COMMENT '자산 이름',
    asset_kind VARCHAR(16) NOT NULL COMMENT 'ASSET 또는 LIABILITY',
    asset_category VARCHAR(32) NOT NULL COMMENT 'STOCK_ETF|DEPOSIT|PENSION|CASH|OTHER|LIABILITY',
    valuation_amount BIGINT NOT NULL COMMENT '평가금액(원)',
    profit_rate DECIMAL(8, 2) NULL COMMENT '손익률(%), 없으면 NULL',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '등록 시각',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 시각',
    PRIMARY KEY (id),
    KEY idx_portfolio_asset_user_amount (user_id, valuation_amount DESC, id DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='사용자 포트폴리오 자산';

CREATE TABLE IF NOT EXISTS portfolio_snapshot (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '월별 기록 PK',
    user_id BIGINT NOT NULL COMMENT '소유 사용자 ID',
    snapshot_month CHAR(7) NOT NULL COMMENT '기록 월',
    total_assets BIGINT NOT NULL COMMENT '기록 시점 총자산(원)',
    total_liabilities BIGINT NOT NULL COMMENT '기록 시점 총부채(원)',
    net_worth BIGINT NOT NULL COMMENT '기록 시점 순자산(원)',
    recorded_at DATETIME(3) NOT NULL COMMENT '기록 시각',
    PRIMARY KEY (id),
    UNIQUE KEY uk_portfolio_snapshot_user_month (user_id, snapshot_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='사용자 포트폴리오 월별 순자산 기록';

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

-- =============================================================================
-- V36: 포트폴리오 공유 좋아요·싫어요·신고
-- -----------------------------------------------------------------------------
-- 목적
--   - 공유 상세에서 회원 한 명당 반응 하나, 신고 하나를 저장
--   - 싫어요가 있으면 경고, 신고가 있으면 블라인드, 좋아요 10개 이상이면 정상으로 본다
-- 대상
--   - portfolio_share_reaction
--   - portfolio_share_report
-- 의존
--   - V33 portfolio_share
--   - V35 moderation_status
-- 연관 API
--   - GET  /api/v1/portfolio/shares/{shareId}
--   - POST /api/v1/portfolio/shares/{shareId}/reactions
--   - POST /api/v1/portfolio/shares/{shareId}/reports
-- 주의
--   - 같은 사용자·같은 카드는 한 행만 허용한다
--   - 삭제된 카드의 본문은 그대로 숨기고, 반응·신고는 받지 않는다
-- =============================================================================

CREATE TABLE IF NOT EXISTS portfolio_share_reaction (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '반응 PK',
    share_id BIGINT NOT NULL COMMENT 'portfolio_share.id',
    user_id BIGINT NOT NULL COMMENT '반응한 사용자 ID',
    reaction_type VARCHAR(16) NOT NULL COMMENT 'LIKE 또는 DISLIKE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '반응 시각',
    PRIMARY KEY (id),
    UNIQUE KEY uk_portfolio_share_reaction_user (share_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='포트폴리오 공유 좋아요·싫어요';

CREATE TABLE IF NOT EXISTS portfolio_share_report (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '신고 PK',
    share_id BIGINT NOT NULL COMMENT 'portfolio_share.id',
    user_id BIGINT NOT NULL COMMENT '신고한 사용자 ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '신고 시각',
    PRIMARY KEY (id),
    UNIQUE KEY uk_portfolio_share_report_user (share_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='포트폴리오 공유 신고';

CREATE TABLE IF NOT EXISTS portfolio_share_comment (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '댓글 PK',
    share_id BIGINT NOT NULL COMMENT 'portfolio_share.id',
    user_id BIGINT NOT NULL COMMENT '작성 사용자 ID',
    author_name VARCHAR(50) NOT NULL COMMENT '작성 당시 이름',
    content VARCHAR(500) NOT NULL COMMENT '댓글 본문',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '작성 시각',
    PRIMARY KEY (id),
    KEY idx_portfolio_share_comment_share (share_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='포트폴리오 공유 댓글';

-- =============================================================================
-- V41: 뉴스 수집 장애 사건과 실행 시도
-- -----------------------------------------------------------------------------
-- 목적
--   - 제공처별 수집 실패를 배치 성공 여부와 분리해 남긴다
--   - 사건 상태와 실행 시도를 따로 기록한다
--   - 같은 제공처의 정기 수집과 재수집이 한 잠금을 원자적으로 선점하게 한다
-- 대상
--   - news_crawl_incident
--   - news_crawl_attempt
--   - news_crawl_provider_lock
-- 연관
--   - NewsCrawlIncidentJpaStore
--   - NewsCrawlLockJpaStore
--   - NewsCrawlingTasklet
--   - NewsProviderRecrawl
-- 주의
--   - 뉴스 본문 행은 지우지 않는다
--   - source_url 유니크 제약(V40)은 그대로 둔다
--   - 잠금 행의 fencing_token 이 바뀌면 이전 실행은 저장하면 안 된다
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 장애 사건
-- 한 행은 제공처 한 곳의 실패다. 배치 잡 성공 여부와 별도로 남긴다.
-- status 는 OPEN → ANALYZED → APPROVED → RUNNING → SUCCEEDED 또는 FAILED 만 허용한다.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS news_crawl_incident (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '장애 사건 PK',
    provider VARCHAR(32) NOT NULL COMMENT '뉴스 제공처. MARKETAUX, ALPHA_VANTAGE, YAHOO_FINANCE',
    status VARCHAR(32) NOT NULL COMMENT 'OPEN, ANALYZED, REJECTED, APPROVED, RUNNING, SUCCEEDED, FAILED',
    cause VARCHAR(32) NOT NULL COMMENT 'CONNECTION, TIMEOUT, HTTP_401, HTTP_403, HTTP_404, HTTP_429, HTTP_5XX, PARSE, UNEXPECTED',
    http_status INT NULL COMMENT 'HTTP 상태 코드. 연결 실패나 타임아웃이면 NULL',
    retry_after_seconds INT NULL COMMENT '429 Retry-After 초. 숫자 헤더가 아니면 NULL',
    message VARCHAR(500) NOT NULL COMMENT '원인 예외 요약. 쿼리의 API 키는 마스킹',
    retryable TINYINT(1) NOT NULL COMMENT '서버 정책상 재수집 허용. 1이면 허용',
    occurred_at DATETIME(6) NOT NULL COMMENT '장애를 관측한 시각',
    analyzed_at DATETIME(6) NULL COMMENT '규칙 분류를 마친 시각',
    model_note VARCHAR(1000) NULL COMMENT '모델 서술. 모델 실패 시 규칙 분류만 유지한다는 문구',
    approved_by VARCHAR(255) NULL COMMENT '승인자 계정',
    approved_at DATETIME(6) NULL COMMENT '승인 시각',
    fencing_token BIGINT NOT NULL COMMENT '실행 선점 토큰. 재선점하면 증가',
    PRIMARY KEY (id),
    KEY idx_news_crawl_incident_provider_status (provider, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='제공처별 뉴스 수집 장애 사건';

-- -----------------------------------------------------------------------------
-- 실행 시도
-- 사건 상태와 별개다. 잠금 충돌로 건너뛴 시도는 SKIPPED 이며 성공이 아니다.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS news_crawl_attempt (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '실행 시도 PK',
    incident_id BIGINT NOT NULL COMMENT 'news_crawl_incident.id',
    status VARCHAR(32) NOT NULL COMMENT 'RUNNING, SUCCEEDED, FAILED, SKIPPED',
    executor VARCHAR(255) NOT NULL COMMENT '재수집을 실행한 계정',
    started_at DATETIME(6) NOT NULL COMMENT '시도 시작 시각',
    finished_at DATETIME(6) NULL COMMENT '시도 종료 시각',
    saved_count INT NOT NULL COMMENT '이번 시도에서 저장한 뉴스 수',
    duplicate_count INT NOT NULL COMMENT '이미 있어 건너뛴 뉴스 수',
    result_message VARCHAR(500) NOT NULL COMMENT '저장 확인 또는 실패 사유',
    PRIMARY KEY (id),
    KEY idx_news_crawl_attempt_incident (incident_id),
    CONSTRAINT fk_news_crawl_attempt_incident
        FOREIGN KEY (incident_id) REFERENCES news_crawl_incident (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='장애 사건별 재수집 시도';

-- -----------------------------------------------------------------------------
-- 제공처 잠금
-- 정기 수집, 수동 수집, 승인 재수집이 같은 제공처에서 동시에 저장하지 않게 한다.
-- expires_at 이 지나면 다른 소유자가 선점하고 fencing_token 이 증가한다.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS news_crawl_provider_lock (
    provider VARCHAR(32) NOT NULL COMMENT '잠금 대상 제공처. PK',
    owner VARCHAR(255) NOT NULL COMMENT '잠금 소유자. batch- 또는 incident- 로 시작',
    fencing_token BIGINT NOT NULL COMMENT '현재 소유자의 펜싱 토큰. 만료 후 재선점하면 증가',
    expires_at DATETIME(6) NOT NULL COMMENT '잠금 만료 시각. 지나면 다른 소유자가 선점',
    PRIMARY KEY (provider)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='제공처별 뉴스 수집 실행 잠금';
