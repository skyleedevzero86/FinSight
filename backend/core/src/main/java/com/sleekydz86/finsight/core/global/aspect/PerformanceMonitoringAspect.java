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
                logger.warn("성능 경고: {}이(가) {}ms 소요됨 (임계값: {}ms)",
                        metricName, executionTime, performanceMonitor.threshold());
            }

            logger.info("성능 지표: {} 실행 시간: {}ms", metricName, executionTime);
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            if (isExpectedClientError(e)) {
                BaseException clientError = (BaseException) e;
                logger.debug("성능 지표: {}이(가) {}ms 후 요청 거부됨 (상태={}, 코드={})",
                        metricName, executionTime, clientError.getHttpStatus(), clientError.getErrorCode());
            } else {
                logger.error("성능 지표: {}이(가) {}ms 후 예외로 실패: {}",
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
