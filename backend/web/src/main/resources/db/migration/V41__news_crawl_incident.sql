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
CREATE TABLE news_crawl_incident (
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
CREATE TABLE news_crawl_attempt (
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
CREATE TABLE news_crawl_provider_lock (
    provider VARCHAR(32) NOT NULL COMMENT '잠금 대상 제공처. PK',
    owner VARCHAR(255) NOT NULL COMMENT '잠금 소유자. batch- 또는 incident- 로 시작',
    fencing_token BIGINT NOT NULL COMMENT '현재 소유자의 펜싱 토큰. 만료 후 재선점하면 증가',
    expires_at DATETIME(6) NOT NULL COMMENT '잠금 만료 시각. 지나면 다른 소유자가 선점',
    PRIMARY KEY (provider)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='제공처별 뉴스 수집 실행 잠금';
