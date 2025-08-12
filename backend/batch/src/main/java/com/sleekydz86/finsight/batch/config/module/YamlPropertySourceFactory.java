package com.sleekydz86.finsight.batch.config.module;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;
import java.util.Properties;

public class YamlPropertySourceFactory extends DefaultPropertySourceFactory {

    @Override
    public PropertiesPropertySource createPropertySource(String name, EncodedResource resource) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(resource.getResource());

        Properties properties = factory.getObject();
        String sourceName = resource.getResource().getFilename();
        if (sourceName == null) {
            sourceName = "unknown";
        }

        return new PropertiesPropertySource(sourceName, properties);
    }
}