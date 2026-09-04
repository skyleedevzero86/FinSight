package com.sleekydz86.finsight.core.global.aspect;

import com.sleekydz86.finsight.core.global.annotation.Retryable;
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
public class RetryAspect {

    private static final Logger logger = LoggerFactory.getLogger(RetryAspect.class);

    @Around("@annotation(retryable)")
    public Object retry(ProceedingJoinPoint joinPoint, Retryable retryable) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();

        int maxAttempts = retryable.maxAttempts();
        long delay = retryable.delay();
        double multiplier = retryable.multiplier();
        long maxDelay = retryable.maxDelay();

        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (attempt > 1) {
                    long currentDelay = Math.min((long) (delay * Math.pow(multiplier, attempt - 2)), maxDelay);
                    logger.info("{}.{} 재시도 (시도 {}/{}) - {}ms 지연 후",
                            className, methodName, attempt, maxAttempts, currentDelay);
                    Thread.sleep(currentDelay);
                }

                return joinPoint.proceed();
            } catch (Exception e) {
                lastException = e;

                if (shouldRetry(e, retryable)) {
                    logger.warn("{}.{} 시도 {}/{} 실패: {}",
                            className, methodName, attempt, maxAttempts, e.getMessage());

                    if (attempt == maxAttempts) {
                        logger.error("{}.{} 총 {}회 시도 모두 실패", className, methodName, maxAttempts);
                        throw e;
                    }
                } else {
                    logger.error("{}.{}에서 재시도 불가 예외: {}", className, methodName, e.getMessage());
                    throw e;
                }
            }
        }

        throw lastException;
    }

    private boolean shouldRetry(Exception e, Retryable retryable) {
        Class<? extends Throwable>[] retryFor = retryable.retryFor();
        Class<? extends Throwable>[] noRetryFor = retryable.noRetryFor();

        if (noRetryFor.length > 0) {
            for (Class<? extends Throwable> noRetryClass : noRetryFor) {
                if (noRetryClass.isAssignableFrom(e.getClass())) {
                    return false;
                }
            }
        }

        if (retryFor.length > 0) {
            for (Class<? extends Throwable> retryClass : retryFor) {
                if (retryClass.isAssignableFrom(e.getClass())) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }
}