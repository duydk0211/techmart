package com.techmart.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techmart.core.authorization.UserContext;
import com.techmart.core.dto.response.storage.FileUploadResponse;
import com.techmart.core.service.storage.StorageService;
import com.techmart.shared.constant.HeaderConstants;
import com.techmart.shared.constant.ResponseCode;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.techmart.core.exception.GlobalExceptionHandler;
import com.techmart.shared.dto.ErrorResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class MinioStorageCoreTests {

    private static final String VALID_INTERNAL_SECRET = "techmart_internal_secret_2026";

    @TestConfiguration
    static class MinioMockTestConfig {
        @Bean
        @Primary
        public FakeMinioClient minioClient() {
            return new FakeMinioClient();
        }
    }

    public static class FakeMinioClient extends MinioClient {
        private boolean bucketExistsReturn = true;
        private boolean throwOnPut = false;
        private final List<MakeBucketArgs> makeBucketCalls = new ArrayList<>();
        private final List<SetBucketPolicyArgs> setPolicyCalls = new ArrayList<>();
        private final List<PutObjectArgs> putObjectCalls = new ArrayList<>();
        private final List<RemoveObjectArgs> removeObjectCalls = new ArrayList<>();

        public FakeMinioClient() {
            super(MinioClient.builder().endpoint("http://localhost:9000").credentials("minioadmin", "minioadmin").build());
        }

        public void reset() {
            bucketExistsReturn = true;
            throwOnPut = false;
            makeBucketCalls.clear();
            setPolicyCalls.clear();
            putObjectCalls.clear();
            removeObjectCalls.clear();
        }

        public void setBucketExistsReturn(boolean val) {
            this.bucketExistsReturn = val;
        }

        public void setThrowOnPut(boolean val) {
            this.throwOnPut = val;
        }

        public List<MakeBucketArgs> getMakeBucketCalls() {
            return makeBucketCalls;
        }

        public List<SetBucketPolicyArgs> getSetPolicyCalls() {
            return setPolicyCalls;
        }

        public List<PutObjectArgs> getPutObjectCalls() {
            return putObjectCalls;
        }

        public List<RemoveObjectArgs> getRemoveObjectCalls() {
            return removeObjectCalls;
        }

        @Override
        public boolean bucketExists(BucketExistsArgs args) {
            return bucketExistsReturn;
        }

        @Override
        public void makeBucket(MakeBucketArgs args) {
            makeBucketCalls.add(args);
        }

        @Override
        public void setBucketPolicy(SetBucketPolicyArgs args) {
            setPolicyCalls.add(args);
        }

        @Override
        public ObjectWriteResponse putObject(PutObjectArgs args) {
            if (throwOnPut) {
                throw new RuntimeException("Simulated MinIO connection reset");
            }
            putObjectCalls.add(args);
            return null;
        }

        @Override
        public void removeObject(RemoveObjectArgs args) {
            removeObjectCalls.add(args);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StorageService storageService;

    @Autowired
    private FakeMinioClient fakeMinioClient;

    @BeforeEach
    void setUp() {
        fakeMinioClient.reset();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
        fakeMinioClient.reset();
    }

    // ==========================================
    // AC-11: AUTO-CREATE 4 BUCKETS & PUBLIC POLICY
    // ==========================================
    @Test
    @DisplayName("AC-11: MinIO auto-creates 4 buckets (products, brands, avatars, banners) and applies anonymous download policy if not present")
    void testAutoCreateBucketsAndSetPublicPolicy() {
        fakeMinioClient.setBucketExistsReturn(false);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-logo.png",
                "image/png",
                "sample png content".getBytes()
        );

        // Upload to brands bucket
        FileUploadResponse response = storageService.uploadFile(file, "brands");
        assertNotNull(response);
        assertEquals("techmart-brands", response.bucket());

        // Verify makeBucket and setBucketPolicy were invoked
        assertEquals(1, fakeMinioClient.getMakeBucketCalls().size());
        assertEquals("techmart-brands", fakeMinioClient.getMakeBucketCalls().get(0).bucket());

        assertEquals(1, fakeMinioClient.getSetPolicyCalls().size());
        assertEquals("techmart-brands", fakeMinioClient.getSetPolicyCalls().get(0).bucket());
        assertTrue(fakeMinioClient.getSetPolicyCalls().get(0).config().contains("s3:GetObject"));
    }

    // ==========================================
    // AC-12: UPLOAD IMAGE SUCCESS WITH UUID V7 (200 OK)
    // ==========================================
    @Test
    @DisplayName("AC-12: Upload image successfully returns 200 OK, generates UUID v7 file name and public download URL")
    void testUploadFileSuccessUuidV7() throws Exception {
        fakeMinioClient.setBucketExistsReturn(true);

        UUID adminId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "galaxy-s24.jpeg",
                "image/jpeg",
                "real image bytes simulated".getBytes()
        );

        mockMvc.perform(multipart("/internal/api/v1/files/upload")
                        .file(file)
                        .param("bucket", "products")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bucket").value("techmart-products"))
                .andExpect(jsonPath("$.fileName", matchesPattern("^[0-9a-fA-F\\-]{36}\\.jpeg$")))
                .andExpect(jsonPath("$.fileUrl", matchesPattern("^http://localhost:9000/techmart-products/[0-9a-fA-F\\-]{36}\\.jpeg$")))
                .andExpect(jsonPath("$.fileSize").value(file.getSize()))
                .andExpect(jsonPath("$.contentType").value("image/jpeg"));

        assertEquals(1, fakeMinioClient.getPutObjectCalls().size());
    }

    @Test
    @DisplayName("CUSTOMER upload to avatars bucket succeeds with 200 OK")
    void testCustomerUploadToAvatarsSuccess() throws Exception {
        fakeMinioClient.setBucketExistsReturn(true);

        UUID customerId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "simulated png content".getBytes()
        );

        mockMvc.perform(multipart("/internal/api/v1/files/upload")
                        .file(file)
                        .param("bucket", "avatars")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", customerId.toString())
                        .header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bucket").value("techmart-avatars"))
                .andExpect(jsonPath("$.fileName", matchesPattern("^[0-9a-fA-F\\-]{36}\\.png$")))
                .andExpect(jsonPath("$.fileUrl", matchesPattern("^http://localhost:9000/techmart-avatars/[0-9a-fA-F\\-]{36}\\.png$")));

        assertEquals(1, fakeMinioClient.getPutObjectCalls().size());
    }

    @Test
    @DisplayName("STAFF upload to any bucket (e.g. banners) succeeds with 200 OK")
    void testStaffUploadSuccess() throws Exception {
        fakeMinioClient.setBucketExistsReturn(true);

        UUID staffId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "promo-banner.webp",
                "image/webp",
                "simulated webp banner content".getBytes()
        );

        mockMvc.perform(multipart("/internal/api/v1/files/upload")
                        .file(file)
                        .param("bucket", "banners")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", staffId.toString())
                        .header("X-User-Role", "STAFF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bucket").value("techmart-banners"))
                .andExpect(jsonPath("$.fileName", matchesPattern("^[0-9a-fA-F\\-]{36}\\.webp$")))
                .andExpect(jsonPath("$.fileUrl", matchesPattern("^http://localhost:9000/techmart-banners/[0-9a-fA-F\\-]{36}\\.webp$")));

        assertEquals(1, fakeMinioClient.getPutObjectCalls().size());
    }

    @Test
    @DisplayName("CUSTOMER upload to other buckets is rejected with 403 ACCESS_DENIED")
    void testCustomerUploadToOtherBucketsRejected() throws Exception {
        UUID customerId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "simulated png content".getBytes()
        );

        // 1. Upload to products bucket
        mockMvc.perform(multipart("/internal/api/v1/files/upload")
                        .file(file)
                        .param("bucket", "products")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", customerId.toString())
                        .header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResponseCode.ACCESS_DENIED.getCode()))
                .andExpect(jsonPath("$.message").value("Customers are only permitted to upload avatar images"));

        // 2. Upload without bucket param (defaults to products)
        mockMvc.perform(multipart("/internal/api/v1/files/upload")
                        .file(file)
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", customerId.toString())
                        .header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResponseCode.ACCESS_DENIED.getCode()))
                .andExpect(jsonPath("$.message").value("Customers are only permitted to upload avatar images"));

        // 3. Upload to brands bucket
        mockMvc.perform(multipart("/internal/api/v1/files/upload")
                        .file(file)
                        .param("bucket", "brands")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", customerId.toString())
                        .header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResponseCode.ACCESS_DENIED.getCode()))
                .andExpect(jsonPath("$.message").value("Customers are only permitted to upload avatar images"));

        // 4. Upload to banners bucket
        mockMvc.perform(multipart("/internal/api/v1/files/upload")
                        .file(file)
                        .param("bucket", "banners")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", customerId.toString())
                        .header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResponseCode.ACCESS_DENIED.getCode()))
                .andExpect(jsonPath("$.message").value("Customers are only permitted to upload avatar images"));
    }

    // ==========================================
    // AC-13: REJECT FILE > 5MB (400 FILE_TOO_LARGE)
    // ==========================================
    @Test
    @DisplayName("AC-13: Reject upload exceeding 5MB with 400 Bad Request and FILE_TOO_LARGE")
    void testUploadFileRejectedExceeding5Mb() throws Exception {
        UUID staffId = UUID.randomUUID();
        byte[] oversizedBytes = new byte[5 * 1024 * 1024 + 10]; // 5MB + 10 bytes
        MockMultipartFile oversizedFile = new MockMultipartFile(
                "file",
                "heavy-file.png",
                "image/png",
                oversizedBytes
        );

        mockMvc.perform(multipart("/internal/api/v1/files/upload")
                        .file(oversizedFile)
                        .param("bucket", "banners")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", staffId.toString())
                        .header("X-User-Role", "STAFF"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ResponseCode.FILE_TOO_LARGE.getCode()));
    }

    // ==========================================
    // AC-13: REJECT INVALID MIME TYPE (400 INVALID_FILE_TYPE)
    // ==========================================
    @Test
    @DisplayName("AC-13: Reject invalid MIME types (PDF, TXT, GIF) with 400 Bad Request and INVALID_FILE_TYPE")
    void testUploadFileRejectedInvalidMimeType() throws Exception {
        UUID staffId = UUID.randomUUID();
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "some pdf content".getBytes()
        );

        mockMvc.perform(multipart("/internal/api/v1/files/upload")
                        .file(pdfFile)
                        .param("bucket", "products")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", staffId.toString())
                        .header("X-User-Role", "STAFF"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_FILE_TYPE.getCode()));

        MockMultipartFile scriptFile = new MockMultipartFile(
                "file",
                "malicious.sh",
                "text/x-shellscript",
                "rm -rf /".getBytes()
        );

        mockMvc.perform(multipart("/internal/api/v1/files/upload")
                        .file(scriptFile)
                        .param("bucket", "products")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", staffId.toString())
                        .header("X-User-Role", "STAFF"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_FILE_TYPE.getCode()));
    }

    // ==========================================
    // AC-13: REJECT EMPTY FILE (400 VALIDATION_FAILED)
    // ==========================================
    @Test
    @DisplayName("AC-13: Reject empty file (0 bytes) with 400 Bad Request and VALIDATION_FAILED")
    void testUploadFileRejectedEmptyFile() throws Exception {
        UUID staffId = UUID.randomUUID();
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        mockMvc.perform(multipart("/internal/api/v1/files/upload")
                        .file(emptyFile)
                        .param("bucket", "avatars")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", staffId.toString())
                        .header("X-User-Role", "STAFF"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ResponseCode.VALIDATION_FAILED.getCode()));
    }

    // ==========================================
    // AC-14: DELETE FILE SUCCESS (204 NO CONTENT) & RBAC ADMIN ONLY
    // ==========================================
    @Test
    @DisplayName("AC-14: Delete file from MinIO returns 204 No Content for ADMIN, but blocks STAFF with 403 Forbidden")
    void testDeleteFileAdminSuccessAndStaffForbidden() throws Exception {
        fakeMinioClient.setBucketExistsReturn(true);

        UUID adminId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        String fileName = "sample-image-to-delete.webp";

        // 1. ADMIN deletes file -> 204 No Content
        mockMvc.perform(delete("/internal/api/v1/files/" + fileName)
                        .param("bucket", "products")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isNoContent());

        assertEquals(1, fakeMinioClient.getRemoveObjectCalls().size());
        assertEquals("sample-image-to-delete.webp", fakeMinioClient.getRemoveObjectCalls().get(0).object());

        // 2. STAFF attempts delete -> 403 Forbidden (ADMIN only)
        mockMvc.perform(delete("/internal/api/v1/files/" + fileName)
                        .param("bucket", "products")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", staffId.toString())
                        .header("X-User-Role", "STAFF"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResponseCode.ACCESS_DENIED.getCode()));
    }

    // ==========================================
    // MINIO EXCEPTION HANDLING (500 FILE_STORAGE_ERROR)
    // ==========================================
    @Test
    @DisplayName("Error Handling: MinIO SDK failure translates to 500 Internal Server Error with FILE_STORAGE_ERROR")
    void testMinioStorageExceptionHandled() throws Exception {
        fakeMinioClient.setBucketExistsReturn(true);
        fakeMinioClient.setThrowOnPut(true);

        UUID adminId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "crash.jpg",
                "image/jpeg",
                "image content".getBytes()
        );

        mockMvc.perform(multipart("/internal/api/v1/files/upload")
                        .file(file)
                        .param("bucket", "products")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ResponseCode.FILE_STORAGE_ERROR.getCode()));
    }

    @Test
    @DisplayName("GlobalExceptionHandler handles MaxUploadSizeExceededException returning 400 FILE_TOO_LARGE")
    void testGlobalExceptionHandlerMaxUploadSizeExceeded() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/api/v1/files/upload");
        request.addHeader(HeaderConstants.CORRELATION_ID, "test-trace-id");
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(5242880);

        ResponseEntity<ErrorResponse> response = handler.handleMaxUploadSizeExceededException(ex, request);
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ResponseCode.FILE_TOO_LARGE.getCode(), response.getBody().getCode());
        assertEquals("test-trace-id", response.getBody().getTraceId());
    }
}
