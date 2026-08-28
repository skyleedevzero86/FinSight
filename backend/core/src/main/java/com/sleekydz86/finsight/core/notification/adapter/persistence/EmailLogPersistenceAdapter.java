package com.sleekydz86.finsight.core.notification.adapter.persistence;

import com.sleekydz86.finsight.core.notification.domain.EmailLog;
import com.sleekydz86.finsight.core.notification.domain.port.in.dto.EmailLogSearchCriteria;
import com.sleekydz86.finsight.core.notification.domain.port.out.EmailLogPersistencePort;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
        return emailLogJpaRepository.findAll(toSpecification(criteria), pageable);
    }

    private Specification<EmailLog> toSpecification(EmailLogSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.keyword() != null) {
                String pattern = "%" + criteria.keyword().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("recipient")), pattern),
                        cb.like(cb.lower(root.get("subject")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("requestIp"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("requestLocation"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("relatedRef"), "")), pattern)));
            }
            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }
            if (criteria.purpose() != null) {
                predicates.add(cb.equal(root.get("purpose"), criteria.purpose()));
            }
            if (criteria.actorType() != null) {
                predicates.add(cb.equal(root.get("actorType"), criteria.actorType()));
            }
            if (criteria.requestIp() != null) {
                predicates.add(cb.equal(root.get("requestIp"), criteria.requestIp()));
            }
            if (criteria.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.from()));
            }
            if (criteria.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), criteria.to()));
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
