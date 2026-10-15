package com.techmart.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.techmart.gateway", "com.techmart.shared"})
public class GatewayServicesApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServicesApplication.class, args);
    }
}
