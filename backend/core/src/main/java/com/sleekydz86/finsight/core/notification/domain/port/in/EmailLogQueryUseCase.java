package com.sleekydz86.finsight.core.notification.domain.port.in;

import com.sleekydz86.finsight.core.notification.domain.port.in.dto.EmailLogSearchCriteria;
import com.sleekydz86.finsight.core.notification.domain.port.out.dto.EmailLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface EmailLogQueryUseCase {

    Page<EmailLogResponse> search(EmailLogSearchCriteria criteria, Pageable pageable);

    Optional<EmailLogResponse> findById(Long id);
}
