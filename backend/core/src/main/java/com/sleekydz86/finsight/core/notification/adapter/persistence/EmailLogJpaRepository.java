package com.sleekydz86.finsight.core.notification.adapter.persistence;

import com.sleekydz86.finsight.core.notification.domain.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmailLogJpaRepository
        extends JpaRepository<EmailLog, Long>, JpaSpecificationExecutor<EmailLog> {
}
