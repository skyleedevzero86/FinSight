package com.sleekydz86.finsight.batch.helper;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.List;

public class DatabaseCleanupListener extends AbstractTestExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(DatabaseCleanupListener.class);

    @Override
    public void beforeTestExecution(TestContext testContext) {
        EntityManager entityManager = testContext.getApplicationContext().getBean(EntityManager.class);
        PlatformTransactionManager transactionManager = testContext.getApplicationContext().getBean(PlatformTransactionManager.class);

        cleanupDatabase(entityManager, transactionManager);
    }

    private void cleanupDatabase(EntityManager entityManager, PlatformTransactionManager transactionManager) {
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());

        try {
            List<String> tableNames = List.of(
                    "news_target_categories", "news", "user_watchlist",
                    "user_notification_preferences", "users", "BATCH_STEP_EXECUTION_CONTEXT",
                    "BATCH_STEP_EXECUTION", "BATCH_JOB_EXECUTION_CONTEXT",
                    "BATCH_JOB_EXECUTION_PARAMS", "BATCH_JOB_EXECUTION", "BATCH_JOB_INSTANCE"
            );

            for (String tableName : tableNames) {
                try {
                    entityManager.createNativeQuery("DELETE FROM " + tableName).executeUpdate();
                } catch (Exception e) {
                    log.debug("테이블 {}을(를) 찾을 수 없거나 이미 비어 있음", tableName);
                }
            }

            transactionManager.commit(status);
            log.info("데이터베이스 정리 완료");
        } catch (Exception e) {
            transactionManager.rollback(status);
            log.error("데이터베이스 정리 실패", e);
        }
    }
}