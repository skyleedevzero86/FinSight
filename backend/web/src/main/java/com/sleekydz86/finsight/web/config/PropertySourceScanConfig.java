package com.sleekydz86.finsight.batch.config.module;

import com.sleekydz86.finsight.web.config.YamlPropertySourceFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@PropertySource(
        value = {
                "classpath:application-core-${spring.profiles.active}.yml"
        },
        factory = YamlPropertySourceFactory.class
)
@Configuration
public class PropertySourceScanConfig {
}