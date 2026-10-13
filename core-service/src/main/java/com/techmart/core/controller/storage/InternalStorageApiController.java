package com.techmart.core.controller.storage;

import com.techmart.core.dto.response.storage.FileUploadResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface InternalStorageApiController {

    ResponseEntity<FileUploadResponse> uploadFile(MultipartFile file, String bucket);

    ResponseEntity<Void> deleteFile(String fileName, String bucket);
}
