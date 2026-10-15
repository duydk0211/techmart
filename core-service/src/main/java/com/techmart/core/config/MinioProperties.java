package com.techmart.core.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Validated configuration properties for MinIO object storage.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "techmart.minio")
public class MinioProperties {

    @NotBlank(message = "MinIO endpoint must not be blank")
    private String endpoint;

    @NotBlank(message = "MinIO access key must not be blank")
    private String accessKey;

    @NotBlank(message = "MinIO secret key must not be blank")
    private String secretKey;
}
