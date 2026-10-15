package com.techmart.core.service.storage;

import com.techmart.core.dto.response.storage.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    FileUploadResponse uploadFile(MultipartFile file, String bucketType);

    void deleteFile(String fileName, String bucketType);
}
