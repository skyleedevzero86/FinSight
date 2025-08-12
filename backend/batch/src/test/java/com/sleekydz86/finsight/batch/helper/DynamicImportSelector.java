package com.sleekydz86.finsight.batch.helper;

import com.sleekydz86.finsight.batch.helper.annotations.JobIntegrationTest;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import java.util.Arrays;
import java.util.Map;

public class DynamicImportSelector implements ImportSelector {

    /**
     * Selects import candidates by reading the `jobClasses` attribute from the
     * `@JobIntegrationTest` annotation on the importing class and returning their
     * fully-qualified class names.
     *
     * If the annotation is not present or its `jobClasses` attribute is missing or
     * not a `Class<?>[]`, an empty array is returned.
     *
     * @param importingClassMetadata metadata of the class that triggered this import selector
     * @return array of fully-qualified class names from `jobClasses`, or an empty array if none
     */
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