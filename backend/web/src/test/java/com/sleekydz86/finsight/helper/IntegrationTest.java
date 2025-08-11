package com.sleekydz86.finsight.helper;

import io.restassured.RestAssured;
import jakarta.persistence.EntityManager;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestExecutionListeners(
        listeners = {
                DependencyInjectionTestExecutionListener.class,
                DatabaseCleanupListener.class
        }
)
public @interface IntegrationTest {
}

class DatabaseCleanupListener extends AbstractTestExecutionListener {

    @Override
    public void beforeTestExecution(TestContext testContext) {
        ApplicationContext applicationContext = testContext.getApplicationContext();
        PlatformTransactionManager transactionManager = applicationContext.getBean(PlatformTransactionManager.class);
        EntityManager entityManager = applicationContext.getBean(EntityManager.class);
        Environment environment = applicationContext.getEnvironment();

        applyRestAssuredPort(environment.getProperty("local.server.port"));
        cleanupDatabase(entityManager, transactionManager);
    }

    private void applyRestAssuredPort(String port) {
        RestAssured.port = Integer.parseInt(
                Objects.requireNonNull(port, "RestAssured port 설정 불가: 'local.server.port'를 사용할 수 없습니다.")
        );
    }

    private void cleanupDatabase(EntityManager entityManager, PlatformTransactionManager transactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            @SuppressWarnings("unchecked")
            List<String> tableNames = (List<String>) entityManager.createNativeQuery(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'"
            ).getResultList();

            List<String> filteredTableNames = tableNames.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .collect(Collectors.toList());

            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();

            for (String tableName : filteredTableNames) {
                entityManager.createNativeQuery("TRUNCATE TABLE \"" + tableName + "\"").executeUpdate();
            }
            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
        });
    }
}