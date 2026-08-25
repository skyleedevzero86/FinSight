CREATE TABLE IF NOT EXISTS email_templates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    html_content LONGTEXT NULL,
    text_content TEXT NULL,
    template_variables JSON NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_email_templates_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
