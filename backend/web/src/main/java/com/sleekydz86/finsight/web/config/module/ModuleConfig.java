package com.sleekydz86.finsight.web.config.module;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
        "com.sleekydz86.core"
})
public class ModuleConfig {
}
