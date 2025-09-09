package com.sleekydz86.finsight.core.global.aspect;

import com.sleekydz86.finsight.core.global.annotation.SecurityAudit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;

@Aspect
@Component
public class SecurityAuditAspect {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditAspect.class);

    @Around("@annotation(securityAudit)")
    public Object auditSecurity(ProceedingJoinPoint joinPoint, SecurityAudit securityAudit) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "anonymous";
        String action = securityAudit.action().isEmpty() ? methodName : securityAudit.action();
        String resource = securityAudit.resource().isEmpty() ? className : securityAudit.resource();

        try {
            if (securityAudit.logRequest()) {
                logger.info("Security audit - User: {}, Action: {}, Resource: {}, Level: {}, Time: {}",
                        username, action, resource, securityAudit.level(), LocalDateTime.now());
            }

            Object result = joinPoint.proceed();

            if (securityAudit.logResponse()) {
                logger.info("Security audit - User: {}, Action: {}, Resource: {}, Result: SUCCESS, Time: {}",
                        username, action, resource, LocalDateTime.now());
            }

            return result;
        } catch (Exception e) {
            logger.error("Security audit - User: {}, Action: {}, Resource: {}, Result: FAILED, Error: {}, Time: {}",
                    username, action, resource, e.getMessage(), LocalDateTime.now());
            throw e;
        }
    }
}