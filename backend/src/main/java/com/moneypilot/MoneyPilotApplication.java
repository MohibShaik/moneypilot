package com.moneypilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point for the MoneyPilot backend application.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MoneyPilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoneyPilotApplication.class, args);
    }
}
