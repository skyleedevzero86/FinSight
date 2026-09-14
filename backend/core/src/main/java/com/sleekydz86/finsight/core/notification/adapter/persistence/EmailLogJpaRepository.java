package com.sleekydz86.finsight.core.notification.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmailLogJpaRepository
        extends JpaRepository<EmailLogJpaEntity, Long>, JpaSpecificationExecutor<EmailLogJpaEntity> {
}
