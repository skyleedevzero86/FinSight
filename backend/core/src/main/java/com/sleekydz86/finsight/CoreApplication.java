package com.sleekydz86.finsight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;

@SpringBootApplication(exclude = BatchAutoConfiguration.class)
public class CoreApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(CoreApplication.class);
        app.setAdditionalProfiles("local", "core-local");
        app.setDefaultProperties(java.util.Map.of("server.port", "8081"));
        app.run(args);
    }

}
