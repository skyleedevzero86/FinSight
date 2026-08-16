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
