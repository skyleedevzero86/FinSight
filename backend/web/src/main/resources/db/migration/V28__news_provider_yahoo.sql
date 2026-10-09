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
