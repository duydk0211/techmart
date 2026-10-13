# Báo cáo triển khai: AdminMetricsIdempotencyMinIO — ver0

## 1. Danh sách thay đổi

### Tạo mới
| File | Mục đích |
|------|----------|
| `shared-library/src/main/java/com/techmart/shared/annotation/Idempotent.java` | Custom Annotation `@Idempotent(expireHours = 24)` cho Idempotency Engine |
| `core-service/src/main/java/com/techmart/core/entity/idempotency/IdempotencyStatus.java` | Enum trạng thái vòng đời idempotency: `IN_PROGRESS`, `COMPLETED`, `FAILED` |
| `core-service/src/main/java/com/techmart/core/entity/idempotency/IdempotencyKeyEntity.java` | JPA Entity ánh xạ bảng `idempotency_keys` |
| `core-service/src/main/java/com/techmart/core/repository/idempotency/IdempotencyKeyRepository.java` | Spring Data JPA Repository cho bảng `idempotency_keys` |
| `core-service/src/main/java/com/techmart/core/service/command/idempotency/IdempotencyService.java` | Interface quản lý giao dịch ghi trạng thái idempotency |
| `core-service/src/main/java/com/techmart/core/service/command/idempotency/impl/IdempotencyServiceImpl.java` | Triển khai `IdempotencyService` với giao dịch `REQUIRES_NEW`, chống race condition và đếm blocked requests |
| `core-service/src/main/java/com/techmart/core/aspect/IdempotencyAspect.java` | Spring AOP Aspect `@Around("@annotation(idempotent)")` kiểm soát replay snapshot, SHA-256 hash và race condition 409 |
| `core-service/src/main/java/com/techmart/core/dto/response/metrics/DashboardSummaryResponse.java` | DTO KPI phong bì: doanh thu hôm nay/hôm qua/tăng trưởng %, đơn hàng phân loại trạng thái, tồn kho, khách hàng |
| `core-service/src/main/java/com/techmart/core/dto/response/metrics/RevenueChartResponse.java` | DTO biểu đồ doanh thu theo chuỗi thời gian kèm zero-filling |
| `core-service/src/main/java/com/techmart/core/dto/response/metrics/StockAlertItemResponse.java` | DTO thông tin biến thể cảnh báo tồn kho (OUT_OF_STOCK, CRITICAL_LOW, LOW_STOCK) |
| `core-service/src/main/java/com/techmart/core/dto/response/metrics/SystemHealthResponse.java` | DTO giám sát hạ tầng thời gian thực: Uptime, JVM Heap, HikariCP Pool, Cache, Idempotency blocked |
| `core-service/src/main/java/com/techmart/core/bus/query/metrics/GetDashboardSummaryQuery.java` | SpringBus Query lấy Dashboard Summary KPI |
| `core-service/src/main/java/com/techmart/core/bus/query/metrics/GetRevenueChartQuery.java` | SpringBus Query lấy biểu đồ doanh thu theo mốc thời gian |
| `core-service/src/main/java/com/techmart/core/bus/query/metrics/GetStockAlertsQuery.java` | SpringBus Query lấy danh sách cảnh báo tồn kho phân trang |
| `core-service/src/main/java/com/techmart/core/bus/query/metrics/GetSystemHealthQuery.java` | SpringBus Query lấy sức khỏe hệ thống thời gian thực |
| `core-service/src/main/java/com/techmart/core/handler/metrics/MetricsHandler.java` | SpringBus Query Handler cho 4 Query đo lường |
| `core-service/src/main/java/com/techmart/core/service/query/metrics/MetricsQueryService.java` | Interface dịch vụ đọc số liệu đo lường |
| `core-service/src/main/java/com/techmart/core/service/query/metrics/impl/MetricsQueryServiceImpl.java` | Triển khai `MetricsQueryService` với Single-Query SQL Aggregation, zero-filling timeline, JVM MXBeans và HikariCP |
| `core-service/src/main/java/com/techmart/core/controller/metrics/InternalMetricsApiController.java` | Interface Controller nội bộ cho 4 API metrics |
| `core-service/src/main/java/com/techmart/core/controller/metrics/impl/InternalMetricsApiControllerImpl.java` | Controller nội bộ `/internal/api/v1/metrics/**` |
| `core-service/src/main/java/com/techmart/core/config/MinioConfig.java` | Cấu hình Bean `MinioClient` từ các biến môi trường |
| `core-service/src/main/java/com/techmart/core/dto/response/storage/FileUploadResponse.java` | DTO trả về kết quả upload tệp tin |
| `core-service/src/main/java/com/techmart/core/service/storage/StorageService.java` | Interface dịch vụ lưu trữ tệp tin |
| `core-service/src/main/java/com/techmart/core/service/storage/impl/MinioStorageServiceImpl.java` | Triển khai MinIO SDK: auto-create 4 buckets, public download policy, upload <= 5MB JPG/PNG/WEBP sinh UUID v7, delete file |
| `core-service/src/main/java/com/techmart/core/controller/storage/InternalStorageApiController.java` | Interface Controller nội bộ lưu trữ tệp tin |
| `core-service/src/main/java/com/techmart/core/controller/storage/impl/InternalStorageApiControllerImpl.java` | Controller nội bộ `/internal/api/v1/files/**` |
| `gateway-services/src/main/java/com/techmart/gateway/controller/admin/GatewayMetricsController.java` | Gateway Controller proxy 4 API `/api/v1/admin/metrics/**` |
| `gateway-services/src/main/java/com/techmart/gateway/controller/storage/GatewayFileController.java` | Gateway Controller proxy `/api/v1/files/upload` và `DELETE /api/v1/files/{fileName}` |

### Chỉnh sửa
| File | Thay đổi |
|------|----------|
| `shared-library/src/main/java/com/techmart/shared/constant/HeaderConstants.java` | Bổ sung hằng số `IDEMPOTENCY_KEY = "Idempotency-Key"` |
| `shared-library/src/main/java/com/techmart/shared/constant/ResponseCode.java` | Bổ sung 5 mã lỗi: `IDEMPOTENCY_KEY_IN_PROGRESS`, `IDEMPOTENCY_KEY_PAYLOAD_MISMATCH`, `FILE_STORAGE_ERROR`, `INVALID_FILE_TYPE`, `FILE_TOO_LARGE` |
| `shared-library/src/main/resources/messages/messages.properties` | Bổ sung thông điệp i18n tiếng Anh cho 5 mã lỗi mới |
| `shared-library/src/main/resources/messages/messages_vi.properties` | Bổ sung thông điệp i18n tiếng Việt cho 5 mã lỗi mới |
| `shared-library/src/test/java/com/techmart/shared/constant/ResponseCodeTest.java` | Cập nhật assertion từ 29 lên 34 ResponseCode enums |
| `shared-library/src/test/java/com/techmart/shared/constant/HeaderConstantsTest.java` | Bổ sung kiểm tra hằng số `IDEMPOTENCY_KEY` |
| `pom.xml` | Khai báo `<minio.version>8.5.17</minio.version>` và `io.minio:minio` trong `<dependencyManagement>` |
| `core-service/pom.xml` | Bổ sung dependency `io.minio:minio` |
| `core-service/src/main/resources/application.yml` | Cấu hình `techmart.minio` (endpoint, credentials, 4 buckets: products, brands, avatars, banners) |
| `core-service/src/test/resources/application.yml` | Cấu hình test an toàn cho `techmart.minio` |
| `core-service/src/main/java/com/techmart/core/controller/order/impl/InternalOrderApiControllerImpl.java` | Gắn `@Idempotent(expireHours = 24)` lên method `createOrder(...)` |
| `gateway-services/src/main/java/com/techmart/gateway/client/CoreClient.java` | Tự động chuyển tiếp header `Idempotency-Key`, hỗ trợ `forwardMultipart` và `forwardBodiless` cho DELETE |
| `gateway-services/src/main/java/com/techmart/gateway/controller/order/GatewayOrderController.java` | Nhận và chuyển tiếp header `Idempotency-Key` khi proxy `POST /api/v1/orders` |
| `gateway-services/src/main/java/com/techmart/gateway/security/GatewaySecurityConfig.java` | Phân quyền RBAC cho metrics (`/system-health` ADMIN only, các metrics khác STAFF/ADMIN) và storage (upload STAFF/ADMIN, delete ADMIN only) |

---

## 2. Logic mới đã bổ sung

1. **Idempotency Engine**:
   - Quản lý vòng đời `IN_PROGRESS` $\to$ `COMPLETED` / `FAILED`.
   - Tính SHA-256 hash của request body đảm bảo an toàn.
   - Giao dịch `REQUIRES_NEW` bảo đảm trạng thái idempotency được commit độc lập với giao dịch nghiệp vụ chính, bảo vệ chống race condition mili-giây với HTTP 409 Conflict.
   - Tự động replay cached snapshot JSON của response khi hash trùng khớp, không gọi lại service nghiệp vụ, không trừ kho lần hai.
   - Trả lời HTTP 422 Unprocessable Entity khi payload bị thay đổi với cùng một idempotency key.
   - Đếm nguyên tử số lượng yêu cầu bị chặn/replay qua `AtomicLong`.

2. **Admin Metrics & Monitoring**:
   - **Dashboard Summary KPI**: Single-Query SQL Aggregation bằng `CASE WHEN` / `FILTER (WHERE ...)` tổng hợp đồng thời doanh thu hôm nay, hôm qua, tỷ lệ tăng trưởng %, và 7 trạng thái đơn hàng trong 1 lần quét; kèm tồn kho khả dụng thấp/hết hàng và lượng khách hàng mới.
   - **Revenue Chart**: Biểu đồ chuỗi thời gian gom nhóm theo ngày (`DATE(created_at)`), áp dụng thuật toán **Zero-Filling** điền đầy đủ các mốc thời gian không có doanh thu để giao diện UI hiển thị mượt mà.
   - **Stock Alerts**: Lọc tồn kho khả dụng $\le$ threshold hoặc $\le$ reorder level, phân loại trạng thái tự động (`OUT_OF_STOCK`, `CRITICAL_LOW`, `LOW_STOCK`), phân trang `PagedResponse`.
   - **System Health**: Giám sát JVM Uptime, JVM Heap Memory (used/max/percent), HikariCP Connection Pool (active, idle, total, awaiting threads), trạng thái cache và số lượng idempotency requests bị chặn.

3. **MinIO Object Storage**:
   - Hỗ trợ 4 buckets chuyên biệt (`techmart-products`, `techmart-brands`, `techmart-avatars`, `techmart-banners`).
   - Tự động kiểm tra và khởi tạo bucket nếu chưa có, gắn anonymous read policy cho phép tải trực tiếp qua public URL.
   - Ràng buộc kích thước file $\le 5\text{MB}$ (`FILE_TOO_LARGE` - 400), kiểm tra định dạng MIME (`image/jpeg`, `image/png`, `image/webp` - `INVALID_FILE_TYPE` - 400).
   - Đặt tên file ngẫu nhiên bằng **UUID v7** kết hợp phần mở rộng gốc để ngăn chặn path traversal và trùng tên.
   - Xóa file trên MinIO với mã trả về HTTP 204 No Content.

4. **Gateway Services Integration**:
   - Forward header `Idempotency-Key` từ Client sang Core Service.
   - Proxy Multipart file upload qua `CoreClient.forwardMultipart`.
   - Proxy DELETE method qua `CoreClient.forwardBodiless`.
   - Phân quyền RBAC Zero-Trust nghiêm ngặt tại `GatewaySecurityConfig`.

---

## 3. Error Handling đã cài đặt

- `IDEMPOTENCY_KEY_IN_PROGRESS` (HTTP 409 Conflict): Khi có request đồng thời đang xử lý cùng một key.
- `IDEMPOTENCY_KEY_PAYLOAD_MISMATCH` (HTTP 422 Unprocessable Entity): Khi gửi lại cùng key nhưng nội dung request body bị thay đổi.
- `FILE_STORAGE_ERROR` (HTTP 500 Internal Server Error): Khi xảy ra lỗi kết nối hoặc thao tác MinIO SDK.
- `INVALID_FILE_TYPE` (HTTP 400 Bad Request): Khi tệp tải lên không phải định dạng JPEG, PNG, hoặc WEBP.
- `FILE_TOO_LARGE` (HTTP 400 Bad Request): Khi kích thước tệp vượt quá 5MB.
- `VALIDATION_FAILED` (HTTP 400 Bad Request): Khi file tải lên rỗng (0 bytes).
- Fallback an toàn cho HikariCP pool khi chạy trên môi trường test không có MXBean.

---

## 4. Ghi chú kỹ thuật

- `IdempotencyServiceImpl` sử dụng `Propagation.REQUIRES_NEW` để đảm bảo lưu vết idempotency ngay cả khi transaction tạo đơn nghiệp vụ bị rollback do lỗi kho hoặc validation.
- SQL Aggregation trong `MetricsQueryServiceImpl` sử dụng `NamedParameterJdbcTemplate` với các tham số kiểu `Timestamp` tương thích 100% cả PostgreSQL thật lẫn in-memory H2 PostgreSQL mode.
- Đặt tên tệp MinIO dùng `Generators.timeBasedEpochGenerator().generate()` (UUID v7) đảm bảo thứ tự thời gian và tính ngẫu nhiên an toàn.

---

## 5. Rules Compliance Report

- ✅ Cấu trúc thư mục: Tuân thủ nghiêm ngặt mô hình CQRS Layer-First (`dto/`, `controller/`, `handler/`, `service/command/`, `service/query/`, `repository/`, `entity/`, `aspect/`).
- ✅ Naming convention: Request/Response/Entity/Repository/Query/Command/Handler đúng chuẩn.
- ✅ SRP (Single Responsibility Principle): Từng service và aspect có trách nhiệm phân định rõ ràng.
- ✅ Bảo mật: Không hardcode credentials, toàn bộ cấu hình MinIO, JWT, DB dùng `${ENV_VAR:default_safe}`.
- ✅ Không dùng FQN inline: Toàn bộ import được khai báo đầy đủ ở đầu file.
- ✅ Logging: Chuẩn `[ClassName][MethodName]` với placeholder `{}` của SLF4J.
- ✅ Kiểm tra cú pháp: Lệnh `mvn test-compile` hoàn tất thành công 100% trên cả 3 module (`BUILD SUCCESS`).
