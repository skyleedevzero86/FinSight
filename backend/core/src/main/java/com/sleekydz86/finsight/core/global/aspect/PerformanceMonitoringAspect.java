package com.sleekydz86.finsight.core.global.aspect;

import com.sleekydz86.finsight.core.global.annotation.PerformanceMonitor;
import com.sleekydz86.finsight.core.global.exception.BaseException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
@Aspect
@Component
public class PerformanceMonitoringAspect {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringAspect.class);

    @Around("@annotation(performanceMonitor)")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint, PerformanceMonitor performanceMonitor) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        String metricName = performanceMonitor.metricName().isEmpty() ?
                className + "." + methodName : performanceMonitor.metricName();

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            if (executionTime > performanceMonitor.threshold()) {
                logger.warn("Performance warning: {} took {}ms (threshold: {}ms)",
                        metricName, executionTime, performanceMonitor.threshold());
            }

            logger.info("Performance metric: {} execution time: {}ms", metricName, executionTime);
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            if (isExpectedClientError(e)) {
                BaseException clientError = (BaseException) e;
                logger.debug("Performance metric: {} rejected after {}ms (status={}, code={})",
                        metricName, executionTime, clientError.getHttpStatus(), clientError.getErrorCode());
            } else {
                logger.error("Performance metric: {} failed after {}ms with exception: {}",
                        metricName, executionTime, e.getMessage(), e);
            }
            throw e;
        }
    }

    private boolean isExpectedClientError(Exception exception) {
        return exception instanceof BaseException baseException
                && baseException.getHttpStatus() >= 400
                && baseException.getHttpStatus() < 500;
    }
}
