package com.hardwarehub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HardwareHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(HardwareHubApplication.class, args);
    }
}
