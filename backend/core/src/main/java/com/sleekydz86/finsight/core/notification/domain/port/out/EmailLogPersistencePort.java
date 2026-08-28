package com.sleekydz86.finsight.core.notification.domain.port.out;

import com.sleekydz86.finsight.core.notification.domain.EmailLog;
import com.sleekydz86.finsight.core.notification.domain.port.in.dto.EmailLogSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface EmailLogPersistencePort {

    EmailLog save(EmailLog emailLog);

    Optional<EmailLog> findById(Long id);

    Page<EmailLog> search(EmailLogSearchCriteria criteria, Pageable pageable);
}
