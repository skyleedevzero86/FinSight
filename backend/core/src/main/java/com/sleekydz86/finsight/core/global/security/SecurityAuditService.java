package com.sleekydz86.finsight.core.global.security;

import com.sleekydz86.finsight.core.global.annotation.SecurityAudit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class SecurityAuditService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditService.class);

    public void logSecurityEvent(String action, String resource, SecurityAudit.SecurityLevel level,
            Object[] args, boolean logRequest, boolean logResponse,
            boolean logUser, String[] sensitiveFields) {

        StringBuilder logMessage = new StringBuilder();
        logMessage.append("Security Event - Action: ").append(action)
                .append(", Resource: ").append(resource)
                .append(", Level: ").append(level);

        if (logRequest && args != null) {
            logMessage.append(", Args: ").append(Arrays.toString(args));
        }

        if (logUser) {
            // 사용자 정보를 가져올예정
            logMessage.append(", User: [Current User]");
        }

        switch (level) {
            case DEBUG:
                logger.debug(logMessage.toString());
                break;
            case INFO:
                logger.info(logMessage.toString());
                break;
            case WARN:
                logger.warn(logMessage.toString());
                break;
            case ERROR:
                logger.error(logMessage.toString());
                break;
        }
    }

    public void logSecurityFailure(String action, String resource, String errorMessage,
            Object[] args, String[] sensitiveFields) {
        logger.error("Security Failure - Action: {}, Resource: {}, Error: {}, Args: {}",
                action, resource, errorMessage, Arrays.toString(args));
    }
}