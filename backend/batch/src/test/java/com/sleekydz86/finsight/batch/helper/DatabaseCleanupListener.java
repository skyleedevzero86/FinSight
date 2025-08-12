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
     * Called before each test execution; performs a full test-time database cleanup.
     *
     * Retrieves required beans from the test application context and invokes the cleanup
     * routine that truncates all tables in the test database schema so each test starts
     * from a clean state.
     *
     * @param testContext the Spring TestContext for the current test execution
     */
    @Override
    public void beforeTestExecution(TestContext testContext) {
        var applicationContext = testContext.getApplicationContext();
        var transactionManager = applicationContext.getBean(PlatformTransactionManager.class);
        var entityManager = applicationContext.getBean(EntityManager.class);

        cleanupDatabase(entityManager, transactionManager);
    }

    /**
     * Truncates all tables in the database PUBLIC schema inside a transaction.
     *
     * <p>Starts a transaction via the provided transaction manager, queries
     * INFORMATION_SCHEMA.TABLES for all table names in the PUBLIC schema, disables
     * referential integrity, truncates each table, then re-enables referential
     * integrity. Intended for use in test setup to produce a clean database state.
     *
     * <p>Runs within a transactional boundary created by a TransactionTemplate;
     * if the transactional block fails the transaction will be rolled back.
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