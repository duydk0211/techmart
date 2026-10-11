package com.techmart.gateway.security;

import com.techmart.gateway.filter.CorrelationIdFilter;
import com.techmart.gateway.filter.JwtAuthenticationFilter;
import com.techmart.gateway.filter.RateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class GatewaySecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorrelationIdFilter correlationIdFilter;
    private final RateLimitFilter rateLimitFilter;
    private final GatewayAuthenticationEntryPoint authenticationEntryPoint;
    private final GatewayAccessDeniedHandler accessDeniedHandler;

    public GatewaySecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                                 CorrelationIdFilter correlationIdFilter,
                                 RateLimitFilter rateLimitFilter,
                                 GatewayAuthenticationEntryPoint authenticationEntryPoint,
                                 GatewayAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.correlationIdFilter = correlationIdFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                
                // --- Phase 2 Public Endpoints ---
                .requestMatchers(HttpMethod.GET, "/api/v1/categories/tree").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/brands").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/branches").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()

                // --- Phase 2 Protected Staff & Admin Endpoints ---
                .requestMatchers(HttpMethod.GET, "/api/v1/branches/*/inventory").hasAnyRole("STAFF", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/branches").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/brands").hasRole("ADMIN")
                .requestMatchers("/api/v1/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasAnyRole("STAFF", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/inventory/adjust").hasAnyRole("STAFF", "ADMIN")

                // --- Phase 1 User & Auth Endpoints ---
                .requestMatchers("/api/v1/users/me/addresses/**").hasRole("CUSTOMER")
                .requestMatchers("/api/v1/auth/logout", "/api/v1/auth/change-password", "/api/v1/users/me/**").authenticated()
                .anyRequest().denyAll()
            )
            .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
