package com.techmart.gateway.controller.auth;

import com.techmart.gateway.client.CoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class GatewayAuthController {

    private static final Logger log = LoggerFactory.getLogger(GatewayAuthController.class);

    private final CoreClient coreClient;

    public GatewayAuthController(CoreClient coreClient) {
        this.coreClient = coreClient;
    }

    @PostMapping("/register")
    public ResponseEntity<Object> register(@RequestBody Object body) {
        log.info("[GatewayAuthController][register] Proxying register request to core-service");
        return coreClient.forward(HttpMethod.POST, "/internal/api/v1/auth/register", body, Object.class);
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody Object body) {
        log.info("[GatewayAuthController][login] Proxying login request to core-service");
        return coreClient.forward(HttpMethod.POST, "/internal/api/v1/auth/login", body, Object.class);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Object> refresh(@RequestBody Object body) {
        log.info("[GatewayAuthController][refresh] Proxying refresh request to core-service");
        return coreClient.forward(HttpMethod.POST, "/internal/api/v1/auth/refresh", body, Object.class);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody Object body) {
        log.info("[GatewayAuthController][logout] Proxying logout request to core-service");
        return coreClient.forward(HttpMethod.POST, "/internal/api/v1/auth/logout", body, Void.class);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@RequestBody Object body) {
        log.info("[GatewayAuthController][changePassword] Proxying change-password request to core-service");
        return coreClient.forward(HttpMethod.POST, "/internal/api/v1/auth/change-password", body, Void.class);
    }
}
