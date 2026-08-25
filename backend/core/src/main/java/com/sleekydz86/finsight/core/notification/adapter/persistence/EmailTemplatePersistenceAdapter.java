package com.sleekydz86.finsight.core.notification.adapter.persistence;

import com.sleekydz86.finsight.core.notification.domain.EmailTemplate;
import com.sleekydz86.finsight.core.notification.domain.port.out.EmailTemplatePersistencePort;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class EmailTemplatePersistenceAdapter implements EmailTemplatePersistencePort {

    private final EmailTemplateJpaRepository emailTemplateJpaRepository;

    public EmailTemplatePersistenceAdapter(EmailTemplateJpaRepository emailTemplateJpaRepository) {
        this.emailTemplateJpaRepository = emailTemplateJpaRepository;
    }

    @Override
    public Optional<EmailTemplate> findActiveByName(String name) {
        return emailTemplateJpaRepository.findByNameAndActiveTrue(name);
    }

    @Override
    public Optional<EmailTemplate> findByName(String name) {
        return emailTemplateJpaRepository.findByName(name);
    }

    @Override
    public EmailTemplate save(EmailTemplate template) {
        return emailTemplateJpaRepository.save(template);
    }

    @Override
    public boolean existsByName(String name) {
        return emailTemplateJpaRepository.findByName(name).isPresent();
    }
}
