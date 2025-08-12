package com.sleekydz86.finsight.batch.config.module;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;
import java.util.Properties;

public class YamlPropertySourceFactory extends DefaultPropertySourceFactory {

    /**
     * Loads YAML from the given EncodedResource and returns it as a PropertiesPropertySource.
     *
     * The YAML resource is parsed into a Properties instance via YamlPropertiesFactoryBean.
     * The returned PropertiesPropertySource is named using the resource filename, or
     * "unknown" if the filename is unavailable.
     *
     * @param name     ignored (not used to determine the property source name)
     * @param resource the EncodedResource pointing to the YAML file to load
     * @return a PropertiesPropertySource containing properties parsed from the YAML resource
     */
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