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

-- 업로드 시각 기준 조회·배치 정리용
CREATE INDEX idx_editor_assets_uploaded_at ON editor_assets (uploaded_at);
