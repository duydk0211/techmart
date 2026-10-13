# Báo cáo kiểm thử: AdminMetricsIdempotencyMinIO — ver0

## Tổng quan
- **Tổng test cases**: 132 (26 test cases mới Phase 4 + 106 test cases hồi quy Phase 00-03)
- **PASS**: 132
- **FAIL**: 0
- **Tỷ lệ pass**: 100%

---

## Chi tiết Test Cases

| # | Test Case | Phân hệ / AC | Loại | Input | Expected | Actual | Kết quả |
|---|-----------|--------------|------|-------|----------|--------|---------|
| 1 | `test01_idempotencyEntityAndTableMapping` | Idempotency / AC-01 | Happy | Entity mapping JPA cho bảng `idempotency_keys` | Table name `idempotency_keys`, Unique constraint trên `idempotency_key`, khóa ngoại `user_id`, enum `IdempotencyStatus` | Khớp 100% entity metadata | ✅ PASS |
| 2 | `test02_firstRequestCreatesOrderAndSavesCompletedSnapshot` | Idempotency / AC-02, AC-03 | Happy | Request tạo đơn với header `Idempotency-Key` mới | Trả về HTTP 201 Created, đơn hàng được tạo, lưu bản ghi COMPLETED với JSON snapshot | HTTP 201, Order code sinh hợp lệ, status COMPLETED, responseBody JSON snapshot được lưu | ✅ PASS |
| 3 | `test03_concurrentRequestsReturn409Conflict` | Idempotency / AC-04 | Edge | 2 request nối tiếp với cùng header `Idempotency-Key` khi trạng thái là IN_PROGRESS | Request thứ hai bị chặn với HTTP 409 Conflict (`Giao dịch đang được xử lý`) | HTTP 409 Conflict, response code `CONFLICT` | ✅ PASS |
| 4 | `test04_multiThreadedConcurrentRequestsOnlyOneSucceeds` | Idempotency / AC-04 | Concurrency | 4 luồng đồng thời gửi cùng `Idempotency-Key` tạo đơn hàng | Đúng 1 luồng thành công (201), các luồng còn lại nhận 409 Conflict | 1 luồng HTTP 201, 3 luồng HTTP 409 Conflict | ✅ PASS |
| 5 | `test05_completedReplayReturnsSnapshotWithoutDoubleDeduction` | Idempotency / AC-05 | Happy / Edge | Gửi lại request với cùng key và body khớp hash sau khi đơn hàng đã COMPLETED | Trả về cached response 201 từ DB, KHÔNG gọi service tạo đơn lần 2, KHÔNG trừ kho 2 lần | HTTP 201 Replay snapshot, tồn kho và số đơn không đổi | ✅ PASS |
| 6 | `test06_payloadHashMismatchReturns422` | Idempotency / AC-06 | Edge | Gửi lại request cùng key nhưng payload body bị thay đổi (sai SHA-256 hash) | Trả về HTTP 422 Unprocessable Entity (`Khóa idempotency này đã được sử dụng cho nội dung đơn hàng khác`) | HTTP 422, response code `PAYLOAD_HASH_MISMATCH` | ✅ PASS |
| 7 | `test07_failedOrderRecoversKeyForRetry` | Idempotency / Edge | Edge | Request tạo đơn gặp lỗi nghiệp vụ (ví dụ: giỏ hàng rỗng / hết hàng) | Cập nhật key về trạng thái FAILED, cho phép gửi lại đơn mới an toàn | Status chuyển FAILED, cho phép retry tạo lại đơn | ✅ PASS |
| 8 | `test08_missingIdempotencyHeaderPassesThrough` | Idempotency / AC-02 | Edge | Request tạo đơn KHÔNG truyền header `Idempotency-Key` | Aspect cho qua (Non-Invasive), tạo đơn bình thường, không ghi bảng idempotency | HTTP 201, tạo đơn thành công, bảng idempotency không có key | ✅ PASS |
| 9 | `test09_expiredKeyReusedSuccessfully` | Idempotency / AC-01 | Edge | Request sử dụng key đã hết hạn 24 giờ (`expires_at < now`) | Cho phép tái sử dụng key để thực hiện phiên giao dịch mới | Khởi tạo thành công phiên tạo đơn mới | ✅ PASS |
| 10 | `test01_getAdminMetricsSummarySuccess` | Admin Metrics / AC-07 | Happy | `GET /api/v1/admin/metrics/summary` với role ADMIN / STAFF | Trả về KPI: doanh thu hôm nay, hôm qua, tăng trưởng %, đơn hàng theo trạng thái, tồn thấp/hết hàng, khách mới | HTTP 200, đầy đủ các trường KPI theo spec | ✅ PASS |
| 11 | `test02_getRevenueChartWithZeroFilling` | Admin Metrics / AC-08 | Happy / Edge | `GET /api/v1/admin/metrics/revenue-chart?period=7_DAYS` | Mảng timeline 7 ngày liên tục, ngày không có doanh thu được zero-fill | HTTP 200, đủ 7 mốc ngày, các ngày trống được điền 0 | ✅ PASS |
| 12 | `test03_getStockAlertsPaginationAndFiltering` | Admin Metrics / AC-09 | Happy / Edge | `GET /api/v1/admin/metrics/stock-alerts?threshold=10&page=0&size=10` | Lọc biến thể tồn kho <= 10, gán nhãn OUT_OF_STOCK / CRITICAL_LOW / LOW_STOCK, phân trang chuẩn | HTTP 200, PagedResponse chuẩn, danh sách cảnh báo tồn chính xác | ✅ PASS |
| 13 | `test04_getSystemHealthMetricsAdminOnly` | Admin Metrics / AC-10 | Happy | `GET /api/v1/admin/metrics/system-health` với role ADMIN | Trả về Uptime, JVM Heap (used, max, %), HikariCP connection pool, Redis ping, Idempotency blocked | HTTP 200, uptime_human, jvm_heap, connection_pool, redis, idempotency metrics | ✅ PASS |
| 14 | `test05_metricsSecurityAccessControl` | Admin Metrics / AC-10 | Edge / Security | Truy cập system-health với STAFF hoặc không có JWT | STAFF nhận 403 Forbidden; Không có JWT nhận 401 Unauthorized | HTTP 403 cho STAFF, HTTP 401 khi thiếu JWT | ✅ PASS |
| 15 | `test01_autoCreateBucketAndDownloadPolicy` | MinIO Storage / AC-11 | Happy | Khởi tạo Service với MinIO Client | Tự động kiểm tra và tạo 4 buckets (`products`, `brands`, `avatars`, `banners`), áp dụng public download policy | 4 buckets được kiểm tra/tạo và gán Anonymous Download Policy | ✅ PASS |
| 16 | `test02_uploadValidImageReturns200AndUuidV7` | MinIO Storage / AC-12 | Happy | Upload ảnh JPEG/PNG hợp lệ lên bucket `products` | Trả về URL ảnh `http://localhost:9000/techmart-products/{uuid-v7}.jpeg` | HTTP 200, tên file định dạng UUID v7 hợp lệ | ✅ PASS |
| 17 | `test03_uploadExceeding5MbRejectedWith400` | MinIO Storage / AC-13 | Edge | Upload file kích thước 5.000.001 bytes (> 5MB) | Trả về HTTP 400 Bad Request, code `FILE_TOO_LARGE` | HTTP 400, response code `FILE_TOO_LARGE` | ✅ PASS |
| 18 | `test04_uploadInvalidMimeTypeRejectedWith400` | MinIO Storage / AC-13 | Edge | Upload file MIME `application/pdf` hoặc `text/x-shellscript` | Trả về HTTP 400 Bad Request, code `INVALID_FILE_TYPE` | HTTP 400, response code `INVALID_FILE_TYPE` | ✅ PASS |
| 19 | `test05_uploadEmptyFileRejectedWith400` | MinIO Storage / AC-13 | Edge | Upload file 0 bytes | Trả về HTTP 400 Bad Request, code `VALIDATION_FAILED` | HTTP 400, response code `VALIDATION_FAILED` | ✅ PASS |
| 20 | `test06_deleteFileAdminSuccessAndStaffForbidden` | MinIO Storage / AC-14 | Happy / Edge | Xóa file với role ADMIN vs role STAFF | ADMIN xóa thành công 204 No Content; STAFF bị từ chối 403 Forbidden | ADMIN nhận HTTP 204; STAFF nhận HTTP 403 Forbidden | ✅ PASS |
| 21 | `test07_minioExceptionHandledAs500FileStorageError` | MinIO Storage / AC-13, ADR-026 | Edge | MinIO SDK ném ngoại lệ khi upload | Bọc và chuyển đổi thành HTTP 500, code `FILE_STORAGE_ERROR` | HTTP 500, response code `FILE_STORAGE_ERROR` | ✅ PASS |
| 22 | `test01_systemHealthAdminOnlyAccess` | Gateway RBAC / AC-15 | Security | Truy cập `/api/v1/admin/metrics/system-health` qua Gateway | Chỉ ADMIN được phép (200), STAFF bị chặn (403), Anonymous bị chặn (401) | ADMIN: 200; STAFF: 403; Anonymous: 401 | ✅ PASS |
| 23 | `test02_metricsSummaryChartAlertsAccessibleByAdminAndStaff` | Gateway RBAC / AC-15 | Security | Truy cập summary, revenue-chart, stock-alerts qua Gateway | Cả ADMIN và STAFF được truy cập (200), CUSTOMER bị chặn (403) | ADMIN & STAFF: 200; CUSTOMER: 403 | ✅ PASS |
| 24 | `test03_fileUploadAccessibleByAdminAndStaff` | Gateway RBAC / AC-15 | Security | Gửi upload file qua Gateway | ADMIN và STAFF được phép upload (200), CUSTOMER bị chặn (403) | ADMIN & STAFF: 200; CUSTOMER: 403 | ✅ PASS |
| 25 | `test04_fileDeleteAdminOnly` | Gateway RBAC / AC-15 | Security | Gửi DELETE file qua Gateway | ADMIN được phép xóa (204), STAFF và CUSTOMER bị chặn (403) | ADMIN: 204; STAFF: 403; CUSTOMER: 403 | ✅ PASS |
| 26 | `test05_orderForwardingIncludesIdempotencyKey` | Gateway RBAC / AC-15 | Integration | Gửi POST `/api/v1/orders` kèm header `Idempotency-Key` qua Gateway | Gateway chuyển tiếp an toàn header `Idempotency-Key` sang Core Service | Request chuyển tiếp có đầy đủ `Idempotency-Key` | ✅ PASS |

---

## Chi tiết lỗi và Quá trình xử lý trong Test Suite

### Lỗi đã xử lý trong quá trình xây dựng Test Suite:
1. **Lỗi Java 24 Mockito Inline Mock Maker trên `MinioClient`:**
   - **Hiện tượng**: Khi dùng `@MockBean private MinioClient minioClient`, ByteBuddy ném ngoại lệ `Could not modify all classes` do hạn chế JVM dynamic agent trên Java 24.
   - **Xử lý**: Triển khai `FakeMinioClient extends MinioClient` với cấu hình in-memory mock sạch sẽ qua `@TestConfiguration`, không phụ thuộc bytecode transformation.
2. **Lỗi `BeanDefinitionOverrideException` khi chạy toàn bộ test suite:**
   - **Hiện tượng**: Khi chạy toàn bộ class trong module `core-service`, Spring Boot phát hiện 2 bean `minioClient` (`MinioConfig` và `MinioMockTestConfig`).
   - **Xử lý**: Bổ sung `properties = "spring.main.allow-bean-definition-overriding=true"` vào `@SpringBootTest` trong `MinioStorageCoreTests`. Test suite chạy hoàn toàn mượt mà 100%.

---

## Rules Compliance Test

| # | Rule | Tuân thủ? | Ghi chú |
|---|------|-----------|---------|
| 1 | Cấu trúc thư mục đúng quy định | ✅ | Phân tách nghiêm ngặt theo CQRS Layer-First: `dto/`, `controller/`, `handler/`, `service/command/`, `service/query/`, `repository/`, `entity/`, `aspect/`. |
| 2 | Naming convention đúng chuẩn | ✅ | Đúng định dạng: DTO kết thúc bằng `Request`/`Response`, Entity `*Entity`, Repository `*Repository`, Service `*Service`/`*ServiceImpl`, Controller `*Controller`. |
| 3 | SRP — không trộn trách nhiệm | ✅ | `IdempotencyAspect` chỉ quản lý AOP idempotency, `MetricsQueryServiceImpl` chỉ xử lý truy vấn số liệu thống kê, `MinioStorageServiceImpl` chỉ xử lý upload/xóa file. |
| 4 | Không hardcode credentials | ✅ | Tất cả thông số MinIO endpoint, access-key, secret-key và DB cấu hình qua `@Value("${...:default}")` an toàn. |
| 5 | Import đúng (không FQN) | ✅ | Toàn bộ import được khai báo ở đầu file, không sử dụng class FQN trong thân code. |
| 6 | Logging đúng format | ✅ | 100% log sử dụng format SLF4J chuẩn `[ClassName][MethodName]` kèm placeholder `{}`. |
| 7 | Hồi quy 106 tests cũ tiếp tục PASS | ✅ | **132/132 tests PASS 100%** (30 tests shared-library + 73 tests core-service + 29 tests gateway-services). Toàn bộ 106 test cũ không bị gãy hay suy thoái. |

---

## Tổng kết Kết quả Test Suites

- **shared-library**: 30/30 tests PASS (100%)
- **core-service**: 73/73 tests PASS (100%)
- **gateway-services**: 29/29 tests PASS (100%)
- **Tổng cộng**: **132/132 tests PASS (100%)**

## Kết luận
**PASS**: Tất cả test cases (26 test mới Phase 4 và 106 test cũ hồi quy) đã hoàn toàn PASS trên toàn bộ hệ thống. Mã nguồn tuân thủ 100% Rules và Tiêu chí nghiệm thu (16 ACs). Sẵn sàng chuyển giao cho Reviewer!
