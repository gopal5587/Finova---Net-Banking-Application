package com.finova;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Finova net-banking backend.
 *
 * <p>Caching and scheduling are enabled application-wide here so that
 * {@code @Cacheable}/{@code @Scheduled} annotations across the codebase are
 * honoured without per-module configuration.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
@EnableScheduling
public class FinovaBankingApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinovaBankingApplication.class, args);
    }
}
