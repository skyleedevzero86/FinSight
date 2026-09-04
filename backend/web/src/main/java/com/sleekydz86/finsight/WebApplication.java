package com.sleekydz86.finsight;

import com.sleekydz86.finsight.core.global.config.ApplicationComponentScanConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Import;

@ConfigurationPropertiesScan(basePackages = {
        "com.sleekydz86.finsight.web",
        "com.sleekydz86.finsight.core"
})
@Import(ApplicationComponentScanConfig.class)
@SpringBootApplication(
        scanBasePackages = "com.sleekydz86.finsight.web",
        exclude = BatchAutoConfiguration.class)
public class WebApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }
}