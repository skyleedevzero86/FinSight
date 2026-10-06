package com.sleekydz86.finsight.core.notification.adapter.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailTemplateJpaRepository extends JpaRepository<EmailTemplateJpaEntity, Long> {

    Optional<EmailTemplateJpaEntity> findByNameAndActiveTrue(String name);

    Optional<EmailTemplateJpaEntity> findByName(String name);
}
