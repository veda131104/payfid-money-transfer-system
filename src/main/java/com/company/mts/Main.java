package com.company.mts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
@Slf4j
public class Main {

    public static void main(String[] args) {
        log.info("====================================================");
        log.info("  PayFid Money Transfer System - Starting Up...");
        log.info("====================================================");

        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);
        Environment env = context.getEnvironment();

        String port = env.getProperty("server.port", "8080");
        String profiles = String.join(", ", env.getActiveProfiles().length > 0 ? env.getActiveProfiles() : new String[]{"default"});
        String datasource = env.getProperty("spring.datasource.url", "N/A");

        log.info("====================================================");
        log.info("  PayFid Money Transfer System STARTED SUCCESSFULLY");
        log.info("  Port:       {}", port);
        log.info("  Profiles:   {}", profiles);
        log.info("  Datasource: {}", datasource);
        log.info("  API Base:   http://localhost:{}/api/v1", port);
        log.info("  Auth:       http://localhost:{}/api/v1/auth/signup", port);
        log.info("  Auth:       http://localhost:{}/api/v1/auth/login", port);
        log.info("====================================================");
    }
}
