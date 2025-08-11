package com.sleekydz86.finsight.web.config.module;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(
        value = {
                "classpath:application-core-${spring.profiles.active}.yml"
        },
        factory = YamlPropertySourceFactory.class
)
public class PropertySourceScanConfig {

}
