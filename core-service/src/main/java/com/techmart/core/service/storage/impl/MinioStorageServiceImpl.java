package com.techmart.core.service.storage.impl;

import com.fasterxml.uuid.Generators;
import com.techmart.core.dto.response.storage.FileUploadResponse;
import com.techmart.core.service.storage.StorageService;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.exception.CoreException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class MinioStorageServiceImpl implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageServiceImpl.class);

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final MinioClient minioClient;
    private final String endpoint;
    private final String productsBucket;
    private final String brandsBucket;
    private final String avatarsBucket;
    private final String bannersBucket;

    public MinioStorageServiceImpl(
            MinioClient minioClient,
            @Value("${techmart.minio.endpoint}") String endpoint,
            @Value("${techmart.minio.buckets.products}") String productsBucket,
            @Value("${techmart.minio.buckets.brands}") String brandsBucket,
            @Value("${techmart.minio.buckets.avatars}") String avatarsBucket,
            @Value("${techmart.minio.buckets.banners}") String bannersBucket) {
        this.minioClient = minioClient;
        this.endpoint = endpoint;
        this.productsBucket = productsBucket;
        this.brandsBucket = brandsBucket;
        this.avatarsBucket = avatarsBucket;
        this.bannersBucket = bannersBucket;
    }

    @Override
    public FileUploadResponse uploadFile(MultipartFile file, String bucketType) {
        log.info("[MinioStorageServiceImpl][uploadFile] Handling upload for bucketType: {}", bucketType);

        if (file == null || file.isEmpty()) {
            log.warn("[MinioStorageServiceImpl][uploadFile] Rejected empty file");
            throw new CoreException(ResponseCode.VALIDATION_FAILED, "Uploaded file must not be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            log.warn("[MinioStorageServiceImpl][uploadFile] Rejected file exceeding 5MB: size={}", file.getSize());
            throw new CoreException(ResponseCode.FILE_TOO_LARGE);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            log.warn("[MinioStorageServiceImpl][uploadFile] Rejected unsupported content type: {}", contentType);
            throw new CoreException(ResponseCode.INVALID_FILE_TYPE);
        }

        String bucketName = resolveBucketName(bucketType);
        ensureBucketExistsAndSetPublicPolicy(bucketName);

        String ext = extractExtension(file.getOriginalFilename(), contentType);
        UUID uuid = Generators.timeBasedEpochGenerator().generate();
        String fileName = uuid + "." + ext;

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );

            String cleanEndpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
            String fileUrl = cleanEndpoint + "/" + bucketName + "/" + fileName;

            log.info("[MinioStorageServiceImpl][uploadFile] File uploaded successfully to URL: {}", fileUrl);
            return new FileUploadResponse(bucketName, fileName, fileUrl, file.getSize(), contentType);
        } catch (Exception e) {
            log.error("[MinioStorageServiceImpl][uploadFile] Error uploading file to MinIO: {}", e.getMessage(), e);
            throw new CoreException(ResponseCode.FILE_STORAGE_ERROR, "Failed to upload file to storage");
        }
    }

    @Override
    public void deleteFile(String fileName, String bucketType) {
        String bucketName = resolveBucketName(bucketType);
        log.info("[MinioStorageServiceImpl][deleteFile] Deleting file: {} from bucket: {}", fileName, bucketName);

        try {
            ensureBucketExistsAndSetPublicPolicy(bucketName);
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
            log.info("[MinioStorageServiceImpl][deleteFile] File removed: {}", fileName);
        } catch (Exception e) {
            log.error("[MinioStorageServiceImpl][deleteFile] Error deleting file from MinIO: {}", e.getMessage(), e);
            throw new CoreException(ResponseCode.FILE_STORAGE_ERROR, "Failed to delete file from storage");
        }
    }

    private String resolveBucketName(String bucketType) {
        if (bucketType == null) {
            return productsBucket;
        }
        return switch (bucketType.toLowerCase()) {
            case "brands" -> brandsBucket;
            case "avatars" -> avatarsBucket;
            case "banners" -> bannersBucket;
            default -> productsBucket;
        };
    }

    private String extractExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
            if (List.of("jpg", "jpeg", "png", "webp").contains(ext)) {
                return "jpg".equals(ext) ? "jpeg" : ext;
            }
        }
        return switch (contentType.toLowerCase()) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpeg";
        };
    }

    private void ensureBucketExistsAndSetPublicPolicy(String bucketName) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                log.info("[MinioStorageServiceImpl][ensureBucket] Creating bucket: {}", bucketName);
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                String policy = """
                    {
                      "Version": "2012-10-17",
                      "Statement": [
                        {
                          "Effect": "Allow",
                          "Principal": {"AWS": ["*"]},
                          "Action": ["s3:GetObject"],
                          "Resource": ["arn:aws:s3:::%s/*"]
                        }
                      ]
                    }
                """.formatted(bucketName);
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucketName).config(policy).build());
            }
        } catch (Exception e) {
            log.error("[MinioStorageServiceImpl][ensureBucket] Failed to initialize bucket: {}", bucketName, e);
            throw new CoreException(ResponseCode.FILE_STORAGE_ERROR, "Failed to initialize storage bucket");
        }
    }
}
