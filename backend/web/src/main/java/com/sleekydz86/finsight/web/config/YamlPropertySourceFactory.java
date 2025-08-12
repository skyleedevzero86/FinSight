package com.sleekydz86.finsight.web.config;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;
import java.io.IOException;
import java.util.Objects;

public class YamlPropertySourceFactory extends DefaultPropertySourceFactory {

    @Override
    public PropertiesPropertySource createPropertySource(
            String name,
            EncodedResource resource
    ) throws IOException {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(resource.getResource());
        return new PropertiesPropertySource(
                Objects.requireNonNull(resource.getResource().getFilename(), "파일 이름은 null일 수 없습니다"),
                Objects.requireNonNull(factory.getObject(), "속성은 null일 수 없습니다")
        );
    }
}
