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
