package com.carpool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties({com.carpool.config.AppProperties.class})
public class CarpoolBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarpoolBackendApplication.class, args);
    }
}
