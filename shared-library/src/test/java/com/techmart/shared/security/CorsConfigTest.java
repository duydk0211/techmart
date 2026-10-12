package com.techmart.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.cors.CorsConfigurationSource;
import jakarta.servlet.Servlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorsConfigTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CorsConfig.class));

    @Test
    void shouldVerifyCorsPropertiesDefaults() {
        CorsProperties properties = new CorsProperties();

        assertFalse(properties.isEnabled());
        assertThat(properties.getAllowedOrigins()).containsExactly("http://localhost:3000");
        assertThat(properties.getAllowedHeaders()).doesNotContain("X-Internal-Secret");
        assertThat(properties.getExposedHeaders()).doesNotContain("X-Internal-Secret");
        assertTrue(properties.isAllowCredentials());
        assertThat(properties.getMaxAge()).isEqualTo(3600L);
    }

    @Test
    void shouldRegisterCorsConfigurationSourceWhenEnabledInServletWeb() {
        contextRunner
                .withPropertyValues("techmart.cors.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(CorsConfigurationSource.class);
                    assertThat(context).hasSingleBean(CorsProperties.class);
                });
    }

    @Test
    void shouldNotRegisterCorsConfigurationSourceWhenDisabled() {
        contextRunner
                .withPropertyValues("techmart.cors.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(CorsConfigurationSource.class);
                });
    }

    @Test
    void shouldNotRegisterCorsConfigurationSourceWhenPropertyMissing() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(CorsConfigurationSource.class);
        });
    }
}
