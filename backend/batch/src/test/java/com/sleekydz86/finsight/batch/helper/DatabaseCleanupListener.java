package com.sleekydz86.finsight.batch.helper.annotations;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;

public class DatabaseCleanupListener extends AbstractTestExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(DatabaseCleanupListener.class);

    /**
     * Invoked before each test execution to ensure the database is cleaned.
     *
     * Retrieves the PlatformTransactionManager and EntityManager from the test
     * application context and delegates to {@link #cleanupDatabase(EntityManager, PlatformTransactionManager)}
     * to truncate all tables in the in-memory database.
     *
     * @param testContext the current Spring TestContext providing the application context
     */
    @Override
    public void beforeTestExecution(TestContext testContext) {
        var applicationContext = testContext.getApplicationContext();
        var transactionManager = applicationContext.getBean(PlatformTransactionManager.class);
        var entityManager = applicationContext.getBean(EntityManager.class);

        cleanupDatabase(entityManager, transactionManager);
    }

    /**
     * Cleans all tables in the database PUBLIC schema within a transactional block.
     *
     * Disables referential integrity, truncates every table discovered via
     * `INFORMATION_SCHEMA.TABLES` (PUBLIC schema), and then re-enables referential
     * integrity. The work is executed via a TransactionTemplate so it runs inside
     * a transaction. Any runtime exceptions thrown while executing the native SQL
     * statements will propagate to the caller.
     */
    private void cleanupDatabase(EntityManager entityManager, PlatformTransactionManager transactionManager) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            @SuppressWarnings("unchecked")
            List<String> tableNames = entityManager
                    .createNativeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'")
                    .getResultList();

            log.debug("Starting database cleanup for {} tables", tableNames.size());

            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();

            for (String tableName : tableNames) {
                entityManager.createNativeQuery("TRUNCATE TABLE \"" + tableName + "\"").executeUpdate();
                log.debug("Truncated table: {}", tableName);
            }

            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
            log.debug("Database cleanup completed");
        });
    }
}