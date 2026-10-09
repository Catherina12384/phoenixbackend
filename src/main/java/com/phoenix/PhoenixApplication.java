package com.phoenix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PhoenixApplication {
    public static void main(String[] args) {
        SpringApplication.run(PhoenixApplication.class, args);
    }
}