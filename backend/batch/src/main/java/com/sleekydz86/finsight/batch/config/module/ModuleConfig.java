package com.sleekydz86.finsight.batch.config.module;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
        "com.sleekydz86.finsight.core",
        "com.sleekydz86.finsight.web"
})
public class ModuleConfig {
}
