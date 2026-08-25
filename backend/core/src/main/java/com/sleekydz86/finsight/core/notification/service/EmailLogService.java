package com.sleekydz86.finsight.core.notification.service;

import com.sleekydz86.finsight.core.notification.domain.EmailLogFactory;
import com.sleekydz86.finsight.core.notification.domain.EmailLogMapper;
import com.sleekydz86.finsight.core.notification.domain.EmailSendContext;
import com.sleekydz86.finsight.core.notification.domain.EmailSendContexts;
import com.sleekydz86.finsight.core.notification.domain.EmailMailPurpose;
import com.sleekydz86.finsight.core.notification.domain.port.in.EmailLogCommandUseCase;
import com.sleekydz86.finsight.core.notification.domain.port.in.EmailLogQueryUseCase;
import com.sleekydz86.finsight.core.notification.domain.port.in.dto.EmailLogSearchCriteria;
import com.sleekydz86.finsight.core.notification.domain.port.in.dto.EmailLogWriteCommand;
import com.sleekydz86.finsight.core.notification.domain.port.out.EmailLogPersistencePort;
import com.sleekydz86.finsight.core.notification.domain.port.out.dto.EmailLogResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class EmailLogService implements EmailLogCommandUseCase, EmailLogQueryUseCase {

    private static final Logger log = LoggerFactory.getLogger(EmailLogService.class);

    private final EmailLogPersistencePort emailLogPersistencePort;

    public EmailLogService(EmailLogPersistencePort emailLogPersistencePort) {
        this.emailLogPersistencePort = emailLogPersistencePort;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(EmailLogWriteCommand command) {
        persist(withDefaultContext(command, null));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(EmailLogWriteCommand command) {
        persist(withDefaultContext(command, command.errorMessage()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmailLogResponse> search(EmailLogSearchCriteria criteria, Pageable pageable) {
        return emailLogPersistencePort.search(criteria, pageable).map(EmailLogMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EmailLogResponse> findById(Long id) {
        return emailLogPersistencePort.findById(id).map(EmailLogMapper::toResponse);
    }

    private void persist(EmailLogWriteCommand command) {
        try {
            EmailSendContext context = command.context();
            String previewSource = context != null && context.bodyPreview() != null
                    ? context.bodyPreview()
                    : command.rawBodyPreview();
            String sanitized = EmailBodyPreviewSanitizer.sanitize(previewSource);
            emailLogPersistencePort.save(EmailLogFactory.fromCommand(command, sanitized));
        } catch (Exception e) {
            log.warn("메일 발송 이력 저장 실패: {}", e.getMessage());
        }
    }

    private EmailLogWriteCommand withDefaultContext(EmailLogWriteCommand command, String errorMessage) {
        EmailSendContext context = command.context() != null
                ? command.context()
                : EmailSendContexts.system(EmailMailPurpose.OTHER);
        return new EmailLogWriteCommand(
                command.recipient(),
                command.subject(),
                command.fromAddress(),
                command.templateType(),
                command.rawBodyPreview(),
                context,
                errorMessage);
    }
}
