package com.techmart.core.dto.response.storage;

/**
 * Metadata and public access URL of an uploaded file.
 */
public record FileUploadResponse(
        String bucket,
        String fileName,
        String fileUrl,
        long fileSize,
        String contentType
) {}
