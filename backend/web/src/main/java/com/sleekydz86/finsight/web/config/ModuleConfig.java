package com.sleekydz86.finsight.web.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
        "com.sleekydz86.core",
        "com.sleekydz86.finsight.web"
})
public class ModuleConfig {
}
