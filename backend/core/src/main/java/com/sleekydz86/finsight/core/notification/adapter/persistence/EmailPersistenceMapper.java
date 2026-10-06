package com.sleekydz86.finsight.core.notification.adapter.persistence;

import com.sleekydz86.finsight.core.notification.domain.EmailLog;
import com.sleekydz86.finsight.core.notification.domain.EmailTemplate;
import com.sleekydz86.finsight.core.notification.domain.WelcomeEmailJob;
import org.springframework.stereotype.Component;

@Component
public class EmailPersistenceMapper {

    public EmailLogJpaEntity toEntity(EmailLog log) {
        return new EmailLogJpaEntity(
                log.getId(),
                log.getRecipient(),
                log.getSubject(),
                log.getTemplateType(),
                log.getPurpose(),
                log.getPurposeLabel(),
                log.getStatus(),
                log.getFromAddress(),
                log.getUserId(),
                log.getActorType(),
                log.getActorUserId(),
                log.getRequestIp(),
                log.getRequestLocation(),
                log.getUserAgent(),
                log.getBodyPreview(),
                log.getErrorMessage(),
                log.getRelatedRef(),
                log.getSentAt(),
                log.getDeliveredAt(),
                log.getOpenedAt(),
                log.getClickedAt(),
                log.getBouncedAt(),
                log.getBounceReason(),
                log.getCreatedAt(),
                log.getUpdatedAt());
    }

    public EmailLog toDomain(EmailLogJpaEntity entity) {
        return EmailLog.restore(
                entity.getId(),
                entity.getRecipient(),
                entity.getSubject(),
                entity.getTemplateType(),
                entity.getPurpose(),
                entity.getPurposeLabel(),
                entity.getStatus(),
                entity.getFromAddress(),
                entity.getUserId(),
                entity.getActorType(),
                entity.getActorUserId(),
                entity.getRequestIp(),
                entity.getRequestLocation(),
                entity.getUserAgent(),
                entity.getBodyPreview(),
                entity.getErrorMessage(),
                entity.getRelatedRef(),
                entity.getSentAt(),
                entity.getDeliveredAt(),
                entity.getOpenedAt(),
                entity.getClickedAt(),
                entity.getBouncedAt(),
                entity.getBounceReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public EmailTemplateJpaEntity toEntity(EmailTemplate template) {
        EmailTemplateJpaEntity entity = new EmailTemplateJpaEntity();
        if (template.getId() != null) {
            entity.setId(template.getId());
        }
        entity.setName(template.getName());
        entity.setSubject(template.getSubject());
        entity.setHtmlContent(template.getHtmlContent());
        entity.setTextContent(template.getTextContent());
        entity.setTemplateVariables(template.getTemplateVariables());
        entity.setActive(template.getActive() != null ? template.getActive() : true);
        entity.setCreatedAt(template.getCreatedAt());
        entity.setUpdatedAt(template.getUpdatedAt());
        return entity;
    }

    public EmailTemplate toDomain(EmailTemplateJpaEntity entity) {
        return EmailTemplate.restore(
                entity.getId(),
                entity.getName(),
                entity.getSubject(),
                entity.getHtmlContent(),
                entity.getTextContent(),
                entity.getTemplateVariables(),
                entity.getActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public void copyTemplate(EmailTemplate source, EmailTemplateJpaEntity target) {
        target.setName(source.getName());
        target.setSubject(source.getSubject());
        target.setHtmlContent(source.getHtmlContent());
        target.setTextContent(source.getTextContent());
        target.setTemplateVariables(source.getTemplateVariables());
        target.setActive(source.getActive() != null ? source.getActive() : true);
    }

    public WelcomeEmailJobJpaEntity toEntity(WelcomeEmailJob job) {
        WelcomeEmailJobJpaEntity entity = new WelcomeEmailJobJpaEntity();
        if (job.getId() != null) {
            entity.setId(job.getId());
        }
        entity.apply(job);
        return entity;
    }

    public WelcomeEmailJob toDomain(WelcomeEmailJobJpaEntity entity) {
        return WelcomeEmailJob.restore(
                entity.getId(),
                entity.getUserId(),
                entity.getRegisteredAt(),
                entity.getDeadlineAt(),
                entity.getScheduledAt(),
                entity.getStatus(),
                entity.getAttemptCount(),
                entity.getLastError(),
                entity.getSentAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
