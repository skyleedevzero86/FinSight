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
@Component
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
                    logger.info("Retrying {}.{} (attempt {}/{}) after {}ms delay",
                            className, methodName, attempt, maxAttempts, currentDelay);
                    Thread.sleep(currentDelay);
                }

                return joinPoint.proceed();
            } catch (Exception e) {
                lastException = e;

                if (shouldRetry(e, retryable)) {
                    logger.warn("Attempt {}/{} failed for {}.{}: {}",
                            attempt, maxAttempts, className, methodName, e.getMessage());

                    if (attempt == maxAttempts) {
                        logger.error("All {} attempts failed for {}.{}", maxAttempts, className, methodName);
                        throw e;
                    }
                } else {
                    logger.error("Non-retryable exception in {}.{}: {}", className, methodName, e.getMessage());
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