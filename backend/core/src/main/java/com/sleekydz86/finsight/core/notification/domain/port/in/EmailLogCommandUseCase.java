package com.sleekydz86.finsight.core.notification.domain.port.in;

import com.sleekydz86.finsight.core.notification.domain.port.in.dto.EmailLogWriteCommand;

public interface EmailLogCommandUseCase {

    void recordSuccess(EmailLogWriteCommand command);

    void recordFailure(EmailLogWriteCommand command);
}
