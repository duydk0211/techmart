package com.techmart.gateway.controller.storage;

import com.techmart.gateway.client.CoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
public class GatewayFileController {

    private static final Logger log = LoggerFactory.getLogger(GatewayFileController.class);

    private final CoreClient coreClient;

    public GatewayFileController(CoreClient coreClient) {
        this.coreClient = coreClient;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bucket", defaultValue = "products") String bucket) {
        log.info("[GatewayFileController][uploadFile] Proxying file upload to core-service for bucket: {}", bucket);
        return coreClient.forwardMultipart("/internal/api/v1/files/upload?bucket=" + bucket, file, "file", null, Object.class);
    }

    @DeleteMapping("/{fileName}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable String fileName,
            @RequestParam(value = "bucket", defaultValue = "products") String bucket) {
        log.info("[GatewayFileController][deleteFile] Proxying file delete to core-service: {} in bucket: {}", fileName, bucket);
        return coreClient.forwardBodiless(HttpMethod.DELETE, "/internal/api/v1/files/" + fileName + "?bucket=" + bucket);
    }
}
