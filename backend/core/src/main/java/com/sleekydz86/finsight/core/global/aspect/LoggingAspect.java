package com.sleekydz86.finsight.core.global.aspect;

import com.sleekydz86.finsight.core.global.annotation.LogExecution;
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
                logger.info("Executing {}.{} with args: {}", className, methodName, Arrays.toString(args));
            } else {
                logger.info("Executing {}.{}", className, methodName);
            }

            Object result = joinPoint.proceed();

            if (logExecution.includeResult()) {
                logger.info("Method {}.{} completed successfully with result: {}", className, methodName, result);
            } else {
                logger.info("Method {}.{} completed successfully", className, methodName);
            }

            return result;
        } catch (Exception e) {
            logger.error("Method {}.{} failed with exception: {}", className, methodName, e.getMessage(), e);
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.info("Method {}.{} execution time: {}ms", className, methodName, executionTime);
        }
    }
}