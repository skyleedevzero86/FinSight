package com.sleekydz86.finsight.core.editor.config;

import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "finsight.editor.minio", name = "enabled", havingValue = "true")
public class EditorMinioConfiguration {

    @Bean
    public MinioClient editorMinioClient(EditorProperties editorProperties) {
        EditorProperties.Minio m = editorProperties.getMinio();
        return MinioClient.builder()
                .endpoint(m.getEndpoint())
                .credentials(m.getAccessKey(), m.getSecretKey())
                .build();
    }
}
