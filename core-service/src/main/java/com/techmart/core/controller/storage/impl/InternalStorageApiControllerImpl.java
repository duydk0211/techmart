package com.techmart.core.controller.storage.impl;

import com.techmart.core.controller.storage.InternalStorageApiController;
import com.techmart.core.dto.response.storage.FileUploadResponse;
import com.techmart.core.service.storage.StorageService;
import com.techmart.shared.security.TechmartPreauthorize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping("/internal/api/v1/files")
public class InternalStorageApiControllerImpl implements InternalStorageApiController {

    private static final Logger log = LoggerFactory.getLogger(InternalStorageApiControllerImpl.class);

    private final StorageService storageService;

    public InternalStorageApiControllerImpl(StorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"STAFF", "ADMIN"})
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bucket", defaultValue = "products") String bucket) {
        log.info("[InternalStorageApiControllerImpl][uploadFile] Uploading file for bucket: {}", bucket);
        FileUploadResponse response = storageService.uploadFile(file, bucket);
        return ResponseEntity.ok(response);
    }

    @Override
    @DeleteMapping("/{fileName}")
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"ADMIN"})
    public ResponseEntity<Void> deleteFile(
            @PathVariable String fileName,
            @RequestParam(value = "bucket", defaultValue = "products") String bucket) {
        log.info("[InternalStorageApiControllerImpl][deleteFile] Deleting file: {} from bucket: {}", fileName, bucket);
        storageService.deleteFile(fileName, bucket);
        return ResponseEntity.noContent().build();
    }
}
