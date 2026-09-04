package com.sleekydz86.finsight.core.global.aspect;

import com.sleekydz86.finsight.core.global.annotation.Transactional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import java.lang.reflect.Method;

@Aspect
@Component
public class TransactionAspect {

    private static final Logger logger = LoggerFactory.getLogger(TransactionAspect.class);

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Around("@annotation(transactional)")
    public Object manageTransaction(ProceedingJoinPoint joinPoint, Transactional transactional) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();

        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(transactional.propagation().getValue());
        def.setIsolationLevel(TransactionDefinition.ISOLATION_DEFAULT);
        def.setTimeout(transactional.timeout());
        def.setReadOnly(transactional.readOnly());

        TransactionStatus status = transactionManager.getTransaction(def);

        try {
            logger.debug("{}.{} 트랜잭션 시작", className, methodName);
            Object result = joinPoint.proceed();
            transactionManager.commit(status);
            logger.debug("{}.{} 트랜잭션 커밋", className, methodName);
            return result;
        } catch (Exception e) {
            if (shouldRollback(e, transactional)) {
                transactionManager.rollback(status);
                logger.debug("{}.{} 트랜잭션 롤백 (원인: {})", className, methodName, e.getMessage());
            } else {
                transactionManager.commit(status);
                logger.debug("{}.{} 예외에도 불구하고 트랜잭션 커밋: {}", className, methodName,
                        e.getMessage());
            }
            throw e;
        }
    }

    private boolean shouldRollback(Exception e, Transactional transactional) {
        String[] rollbackFor = transactional.rollbackFor();
        String[] noRollbackFor = transactional.noRollbackFor();

        if (noRollbackFor.length > 0) {
            for (String noRollbackClassName : noRollbackFor) {
                if (isAssignableFrom(e.getClass(), noRollbackClassName)) {
                    return false;
                }
            }
        }

        if (rollbackFor.length > 0) {
            for (String rollbackClassName : rollbackFor) {
                if (isAssignableFrom(e.getClass(), rollbackClassName)) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }

    private boolean isAssignableFrom(Class<?> exceptionClass, String className) {
        try {
            Class<?> targetClass = Class.forName(className);
            return targetClass.isAssignableFrom(exceptionClass);
        } catch (ClassNotFoundException e) {
            logger.warn("클래스를 찾을 수 없음: {}", className);
            return false;
        }
    }
}