package com.sleekydz86.finsight.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

@TestConfiguration
@ComponentScan(basePackages = "com.sleekydz86.finsight.core")
@ActiveProfiles("test")
public class TestConfig {
}