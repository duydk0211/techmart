package com.techmart.core.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${techmart.minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${techmart.minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${techmart.minio.secret-key:minioadmin}")
    private String secretKey;

    @Bean
    @ConditionalOnMissingBean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
