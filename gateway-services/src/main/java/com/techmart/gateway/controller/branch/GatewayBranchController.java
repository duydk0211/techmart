package com.techmart.gateway.controller.branch;

import com.techmart.gateway.client.CoreClient;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/branches")
public class GatewayBranchController {

    private static final Logger log = LoggerFactory.getLogger(GatewayBranchController.class);

    private final CoreClient coreClient;

    public GatewayBranchController(CoreClient coreClient) {
        this.coreClient = coreClient;
    }

    @GetMapping
    public ResponseEntity<Object> getBranchList() {
        log.info("[GatewayBranchController][getBranchList] Proxying public getBranchList to core-service");
        return coreClient.forward(HttpMethod.GET, "/internal/api/v1/branches", null, Object.class);
    }

    @PostMapping
    public ResponseEntity<Object> createBranch(@RequestBody Object body) {
        log.info("[GatewayBranchController][createBranch] Proxying createBranch to core-service");
        return coreClient.forward(HttpMethod.POST, "/internal/api/v1/branches", body, Object.class);
    }

    @GetMapping("/{id}/inventory")
    public ResponseEntity<Object> getBranchInventory(@PathVariable("id") UUID id, HttpServletRequest request) {
        log.info("[GatewayBranchController][getBranchInventory] Proxying getBranchInventory to core-service: {}", id);
        String query = request.getQueryString();
        String uri = "/internal/api/v1/branches/" + id + "/inventory" + (query != null ? "?" + query : "");
        return coreClient.forward(HttpMethod.GET, uri, null, Object.class);
    }
}
