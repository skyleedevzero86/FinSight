package com.sleekydz86.finsight.batch.helper.annotations;

import com.sleekydz86.finsight.batch.helper.DynamicImportSelector;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@TestExecutionListeners(
        listeners = {
                DependencyInjectionTestExecutionListener.class,
                com.sleekydz86.finsight.batch.helper.annotations.DatabaseCleanupListener.class
        }
)
@Import(DynamicImportSelector.class)
@SpringBatchTest
@SpringBootTest
public @interface JobIntegrationTest {
    Class<?>[] jobClasses();
}