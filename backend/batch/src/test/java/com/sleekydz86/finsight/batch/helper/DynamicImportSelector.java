package com.sleekydz86.finsight.batch.helper;

import com.sleekydz86.finsight.batch.helper.annotations.JobIntegrationTest;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import java.util.Arrays;
import java.util.Map;

public class DynamicImportSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        Map<String, Object> annotationAttributes = importingClassMetadata
                .getAnnotationAttributes(JobIntegrationTest.class.getName());

        if (annotationAttributes == null) {
            return new String[0];
        }

        Object value = annotationAttributes.get("jobClasses");

        if (value instanceof Class<?>[]) {
            Class<?>[] classes = (Class<?>[]) value;
            return Arrays.stream(classes)
                    .map(Class::getName)
                    .toArray(String[]::new);
        }

        return new String[0];
    }
}