package com.sleekydz86.finsight.core.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationPropertiesScan("com.sleekydz86.finsight.core.news.adapter.requester")
public class PropertiesScanConfig {
}