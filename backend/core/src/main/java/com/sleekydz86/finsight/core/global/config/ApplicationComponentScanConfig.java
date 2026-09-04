package com.sleekydz86.finsight.core.global.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
@ComponentScan(basePackages = "com.sleekydz86.finsight.core")
public class ApplicationComponentScanConfig {

}
