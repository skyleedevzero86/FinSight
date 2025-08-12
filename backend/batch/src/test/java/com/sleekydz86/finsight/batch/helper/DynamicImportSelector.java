package com.sleekydz86.finsight.batch.helper;

import com.sleekydz86.finsight.batch.helper.annotations.JobIntegrationTest;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import java.util.Arrays;
import java.util.Map;

public class DynamicImportSelector implements ImportSelector {

    /**
     * Resolve imports from the {@code JobIntegrationTest} annotation on the importing class.
     *
     * Examines the importing class's annotation attributes for a {@code jobClasses} attribute.
     * If present and of type {@code Class<?>[]}, returns the fully-qualified class names of those
     * classes. If the annotation is absent or the attribute has an unexpected type, returns an
     * empty array.
     *
     * @param importingClassMetadata metadata for the class that triggered this import selection
     * @return an array of fully-qualified class names to import, or an empty array if none found
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