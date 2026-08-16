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
