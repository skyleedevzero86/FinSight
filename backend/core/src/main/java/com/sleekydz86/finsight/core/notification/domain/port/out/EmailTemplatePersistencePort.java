package com.sleekydz86.finsight.core.notification.domain.port.out;

import com.sleekydz86.finsight.core.notification.domain.EmailTemplate;

import java.util.Optional;

public interface EmailTemplatePersistencePort {

    Optional<EmailTemplate> findActiveByName(String name);

    Optional<EmailTemplate> findByName(String name);

    EmailTemplate save(EmailTemplate template);

    boolean existsByName(String name);
}
