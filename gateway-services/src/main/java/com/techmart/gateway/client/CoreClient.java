package com.techmart.gateway.client;

import com.techmart.shared.constant.HeaderConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class CoreClient {

    private static final Logger log = LoggerFactory.getLogger(CoreClient.class);

    private final RestClient restClient;
    private final String internalSecret;

    public CoreClient(
            @Value("${techmart.core-service.url:http://localhost:8081}") String coreUrl,
            @Value("${techmart.security.internal-secret:techmart_internal_secret_2026}") String internalSecret) {
        this.restClient = RestClient.builder()
                .baseUrl(coreUrl)
                .build();
        this.internalSecret = internalSecret;
    }

    public <T, R> ResponseEntity<R> forward(HttpMethod method, String uri, T body, Class<R> responseType) {
        RestClient.RequestBodySpec spec = prepareRequest(method, uri);
        if (body != null) {
            spec.contentType(MediaType.APPLICATION_JSON);
            spec.body(body);
        }
        return spec.retrieve().toEntity(responseType);
    }

    public <T, R> ResponseEntity<R> forward(HttpMethod method, String uri, T body, ParameterizedTypeReference<R> responseType) {
        RestClient.RequestBodySpec spec = prepareRequest(method, uri);
        if (body != null) {
            spec.contentType(MediaType.APPLICATION_JSON);
            spec.body(body);
        }
        return spec.retrieve().toEntity(responseType);
    }

    private RestClient.RequestBodySpec prepareRequest(HttpMethod method, String uri) {
        String correlationId = MDC.get(HeaderConstants.CORRELATION_ID);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        RestClient.RequestBodySpec spec = restClient.method(method)
                .uri(uri)
                .header(HeaderConstants.X_INTERNAL_SECRET, internalSecret)
                .header(HeaderConstants.CORRELATION_ID, correlationId != null ? correlationId : "");

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            spec.header("X-User-Id", auth.getName());

            String role = null;
            for (GrantedAuthority authority : auth.getAuthorities()) {
                String authStr = authority.getAuthority();
                if (authStr.startsWith("ROLE_")) {
                    role = authStr.substring(5);
                    break;
                } else {
                    role = authStr;
                }
            }
            if (role != null) {
                spec.header("X-User-Role", role);
            }

            if (auth.getDetails() instanceof Map<?, ?> detailsMap) {
                Object email = detailsMap.get("email");
                if (email != null) {
                    spec.header("X-User-Email", email.toString());
                }
            }
        }

        return spec;
    }
}
