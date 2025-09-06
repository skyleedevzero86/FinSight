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
        def.setPropagationBehavior(transactional.propagation().ordinal());
        def.setIsolationLevel(TransactionDefinition.ISOLATION_DEFAULT);
        def.setTimeout(transactional.timeout());
        def.setReadOnly(transactional.readOnly());

        TransactionStatus status = transactionManager.getTransaction(def);

        try {
            logger.debug("Starting transaction for {}.{}", className, methodName);
            Object result = joinPoint.proceed();
            transactionManager.commit(status);
            logger.debug("Transaction committed for {}.{}", className, methodName);
            return result;
        } catch (Exception e) {
            if (shouldRollback(e, transactional)) {
                transactionManager.rollback(status);
                logger.debug("Transaction rolled back for {}.{} due to: {}", className, methodName, e.getMessage());
            } else {
                transactionManager.commit(status);
                logger.debug("Transaction committed for {}.{} despite exception: {}", className, methodName, e.getMessage());
            }
            throw e;
        }
    }

    private boolean shouldRollback(Exception e, Transactional transactional) {
        Class<? extends Throwable>[] rollbackFor = transactional.rollbackFor();
        Class<? extends Throwable>[] noRollbackFor = transactional.noRollbackFor();

        if (noRollbackFor.length > 0) {
            for (Class<? extends Throwable> noRollbackClass : noRollbackFor) {
                if (noRollbackClass.isAssignableFrom(e.getClass())) {
                    return false;
                }
            }
        }

        if (rollbackFor.length > 0) {
            for (Class<? extends Throwable> rollbackClass : rollbackFor) {
                if (rollbackClass.isAssignableFrom(e.getClass())) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }
}