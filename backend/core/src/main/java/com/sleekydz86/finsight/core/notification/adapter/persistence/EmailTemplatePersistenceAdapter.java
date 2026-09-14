package com.sleekydz86.finsight.core.notification.adapter.persistence;

import com.sleekydz86.finsight.core.notification.domain.EmailTemplate;
import com.sleekydz86.finsight.core.notification.domain.port.out.EmailTemplatePersistencePort;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class EmailTemplatePersistenceAdapter implements EmailTemplatePersistencePort {

    private final EmailTemplateJpaRepository emailTemplateJpaRepository;
    private final EmailPersistenceMapper emailPersistenceMapper;

    public EmailTemplatePersistenceAdapter(
            EmailTemplateJpaRepository emailTemplateJpaRepository,
            EmailPersistenceMapper emailPersistenceMapper) {
        this.emailTemplateJpaRepository = emailTemplateJpaRepository;
        this.emailPersistenceMapper = emailPersistenceMapper;
    }

    @Override
    public Optional<EmailTemplate> findActiveByName(String name) {
        return emailTemplateJpaRepository.findByNameAndActiveTrue(name)
                .map(emailPersistenceMapper::toDomain);
    }

    @Override
    public Optional<EmailTemplate> findByName(String name) {
        return emailTemplateJpaRepository.findByName(name)
                .map(emailPersistenceMapper::toDomain);
    }

    @Override
    public EmailTemplate save(EmailTemplate template) {
        EmailTemplateJpaEntity entity;
        if (template.getId() != null) {
            entity = emailTemplateJpaRepository.findById(template.getId())
                    .orElseGet(EmailTemplateJpaEntity::new);
            emailPersistenceMapper.copyTemplate(template, entity);
        } else if (template.getName() != null) {
            entity = emailTemplateJpaRepository.findByName(template.getName())
                    .orElseGet(EmailTemplateJpaEntity::new);
            emailPersistenceMapper.copyTemplate(template, entity);
        } else {
            entity = emailPersistenceMapper.toEntity(template);
        }
        EmailTemplateJpaEntity saved = emailTemplateJpaRepository.save(entity);
        return emailPersistenceMapper.toDomain(saved);
    }

    @Override
    public boolean existsByName(String name) {
        return emailTemplateJpaRepository.findByName(name).isPresent();
    }
}
