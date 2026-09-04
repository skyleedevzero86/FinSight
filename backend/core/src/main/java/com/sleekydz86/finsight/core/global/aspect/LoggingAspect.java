package com.sleekydz86.finsight.core.global.aspect;

import com.sleekydz86.finsight.core.global.annotation.LogExecution;
import com.sleekydz86.finsight.core.global.exception.BaseException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("@annotation(logExecution)")
    public Object logExecution(ProceedingJoinPoint joinPoint, LogExecution logExecution) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();

        long startTime = System.currentTimeMillis();

        try {
            if (logExecution.includeArgs()) {
                Object[] args = joinPoint.getArgs();
                logger.info("{}.{} 실행 중 (인자: {})", className, methodName, Arrays.toString(args));
            } else {
                logger.info("{}.{} 실행 중", className, methodName);
            }

            Object result = joinPoint.proceed();

            if (logExecution.includeResult()) {
                logger.info("메서드 {}.{} 정상 완료 (결과: {})", className, methodName, result);
            } else {
                logger.info("메서드 {}.{} 정상 완료", className, methodName);
            }

            return result;
        } catch (Exception e) {
            if (isExpectedClientError(e)) {
                BaseException clientError = (BaseException) e;
                logger.debug("메서드 {}.{} 요청 거부 (상태={}, 코드={})",
                        className, methodName, clientError.getHttpStatus(), clientError.getErrorCode());
            } else {
                logger.error("메서드 {}.{} 예외로 실패: {}", className, methodName, e.getMessage(), e);
            }
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.info("메서드 {}.{} 실행 시간: {}ms", className, methodName, executionTime);
        }
    }

    private boolean isExpectedClientError(Exception exception) {
        return exception instanceof BaseException baseException
                && baseException.getHttpStatus() >= 400
                && baseException.getHttpStatus() < 500;
    }
}
