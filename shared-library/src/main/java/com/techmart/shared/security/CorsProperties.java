package com.techmart.shared.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for CORS settings under 'techmart.cors'.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "techmart.cors")
public class CorsProperties {

    private boolean enabled = false;
    private List<String> allowedOrigins = List.of("http://localhost:3000");
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    private List<String> allowedHeaders = List.of("Authorization", "Content-Type", "Accept-Language", "Correlation-Id", "X-Correlation-Id");
    private List<String> exposedHeaders = List.of("Authorization", "Correlation-Id", "X-Correlation-Id");
    private boolean allowCredentials = true;
    private long maxAge = 3600L;
}
