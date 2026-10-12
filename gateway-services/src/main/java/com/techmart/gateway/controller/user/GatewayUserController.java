package com.techmart.gateway.controller.user;

import com.techmart.gateway.client.CoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me")
public class GatewayUserController {

    private static final Logger log = LoggerFactory.getLogger(GatewayUserController.class);

    private final CoreClient coreClient;

    public GatewayUserController(CoreClient coreClient) {
        this.coreClient = coreClient;
    }

    @GetMapping
    public ResponseEntity<Object> getProfile() {
        log.info("[GatewayUserController][getProfile] Proxying getProfile to core-service");
        return coreClient.forward(HttpMethod.GET, "/internal/api/v1/user/profile", null, Object.class);
    }

    @PutMapping
    public ResponseEntity<Object> updateProfile(@RequestBody Object body) {
        log.info("[GatewayUserController][updateProfile] Proxying updateProfile to core-service");
        return coreClient.forward(HttpMethod.PUT, "/internal/api/v1/user/profile", body, Object.class);
    }

    @GetMapping("/addresses")
    public ResponseEntity<Object> getAddresses() {
        log.info("[GatewayUserController][getAddresses] Proxying getAddresses to core-service");
        return coreClient.forward(HttpMethod.GET, "/internal/api/v1/user/addresses", null, Object.class);
    }

    @PostMapping("/addresses")
    public ResponseEntity<Object> addAddress(@RequestBody Object body) {
        log.info("[GatewayUserController][addAddress] Proxying addAddress to core-service");
        return coreClient.forward(HttpMethod.POST, "/internal/api/v1/user/addresses", body, Object.class);
    }

    @PutMapping("/addresses/{id}")
    public ResponseEntity<Object> updateAddress(@PathVariable("id") UUID id, @RequestBody Object body) {
        log.info("[GatewayUserController][updateAddress] Proxying updateAddress to core-service for id: {}", id);
        return coreClient.forward(HttpMethod.PUT, "/internal/api/v1/user/addresses/" + id, body, Object.class);
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable("id") UUID id) {
        log.info("[GatewayUserController][deleteAddress] Proxying deleteAddress to core-service for id: {}", id);
        return coreClient.forward(HttpMethod.DELETE, "/internal/api/v1/user/addresses/" + id, null, Void.class);
    }

    @PatchMapping("/addresses/{id}/default")
    public ResponseEntity<Object> setDefaultAddress(@PathVariable("id") UUID id) {
        log.info("[GatewayUserController][setDefaultAddress] Proxying setDefaultAddress to core-service for id: {}", id);
        return coreClient.forward(HttpMethod.PATCH, "/internal/api/v1/user/addresses/" + id + "/default", null, Object.class);
    }
}
