package com.sleekydz86.finsight.batch.config.module;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;
import java.util.Properties;

public class YamlPropertySourceFactory extends DefaultPropertySourceFactory {

    /**
     * Creates a PropertiesPropertySource from a YAML resource.
     *
     * <p>Parses the provided YAML EncodedResource into a Properties object and returns a
     * PropertiesPropertySource whose name is derived from the resource file name. If the
     * resource has no filename, the source name defaults to "unknown". The incoming
     * `name` parameter is ignored.
     *
     * @param name     the name supplied by the caller (ignored)
     * @param resource the YAML resource to load into a property source
     * @return a PropertiesPropertySource backed by properties parsed from the YAML resource
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