package com.sleekydz86.finsight.core.notification.domain;

import java.time.LocalDateTime;

public class EmailTemplate {

    private Long id;
    private String name;
    private String subject;
    private String htmlContent;
    private String textContent;
    private String templateVariables;
    private Boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public EmailTemplate() {
    }

    public static EmailTemplate restore(
            Long id,
            String name,
            String subject,
            String htmlContent,
            String textContent,
            String templateVariables,
            Boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        EmailTemplate template = new EmailTemplate();
        template.id = id;
        template.name = name;
        template.subject = subject;
        template.htmlContent = htmlContent;
        template.textContent = textContent;
        template.templateVariables = templateVariables;
        template.active = active != null ? active : true;
        template.createdAt = createdAt;
        template.updatedAt = updatedAt;
        return template;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public String getTemplateVariables() {
        return templateVariables;
    }

    public void setTemplateVariables(String templateVariables) {
        this.templateVariables = templateVariables;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
