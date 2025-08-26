package com.sleekydz86.finsight.batch.helper.annotations;

import com.sleekydz86.finsight.batch.helper.DatabaseCleanupListener;
import com.sleekydz86.finsight.batch.helper.DynamicImportSelector;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestExecutionListeners;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@TestExecutionListeners(
        listeners = {
                org.springframework.test.context.support.DependencyInjectionTestExecutionListener.class,
                DatabaseCleanupListener.class
        }
)
@Import(DynamicImportSelector.class)
@SpringBatchTest
@SpringBootTest
public @interface JobIntegrationTest {
    Class<?>[] jobClasses();
}