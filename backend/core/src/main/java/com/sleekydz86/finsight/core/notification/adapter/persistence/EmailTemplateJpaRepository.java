package com.sleekydz86.finsight.core.notification.adapter.persistence;

import com.sleekydz86.finsight.core.notification.domain.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailTemplateJpaRepository extends JpaRepository<EmailTemplate, Long> {

    Optional<EmailTemplate> findByNameAndActiveTrue(String name);

    Optional<EmailTemplate> findByName(String name);
}
