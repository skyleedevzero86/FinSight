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
        logMessage.append("보안 이벤트 - 액션: ").append(action)
                .append(", 리소스: ").append(resource)
                .append(", 수준: ").append(level);

        if (logRequest && args != null) {
            logMessage.append(", 인자: ").append(Arrays.toString(args));
        }

        if (logUser) {
            
            logMessage.append(", 사용자: [현재 사용자]");
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
        logger.error("보안 실패 - 액션: {}, 리소스: {}, 오류: {}, 인자: {}",
                action, resource, errorMessage, Arrays.toString(args));
    }
}