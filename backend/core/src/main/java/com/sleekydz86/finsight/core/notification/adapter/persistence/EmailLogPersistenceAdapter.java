package com.sleekydz86.finsight.core.notification.adapter.persistence;

import com.sleekydz86.finsight.core.notification.domain.EmailLog;
import com.sleekydz86.finsight.core.notification.domain.port.in.dto.EmailLogSearchCriteria;
import com.sleekydz86.finsight.core.notification.domain.port.out.EmailLogPersistencePort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class EmailLogPersistenceAdapter implements EmailLogPersistencePort {

    private final EmailLogJpaRepository emailLogJpaRepository;

    public EmailLogPersistenceAdapter(EmailLogJpaRepository emailLogJpaRepository) {
        this.emailLogJpaRepository = emailLogJpaRepository;
    }

    @Override
    public EmailLog save(EmailLog emailLog) {
        return emailLogJpaRepository.save(emailLog);
    }

    @Override
    public Optional<EmailLog> findById(Long id) {
        return emailLogJpaRepository.findById(id);
    }

    @Override
    public Page<EmailLog> search(EmailLogSearchCriteria criteria, Pageable pageable) {
        return emailLogJpaRepository.search(
                criteria.keyword(),
                criteria.status(),
                criteria.purpose(),
                criteria.actorType(),
                criteria.requestIp(),
                criteria.from(),
                criteria.to(),
                pageable);
    }
}
