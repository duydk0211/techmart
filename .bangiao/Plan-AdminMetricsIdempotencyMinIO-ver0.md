# Kế hoạch triển khai: AdminMetricsIdempotencyMinIO — ver0
## Hệ thống TechMart — Electronic Store Management System
### Phase 4: Admin Metrics & Monitoring, Idempotency Engine & MinIO Object Storage

> **Tài liệu tham chiếu:**
> - `AI_Agent/plans/.bangiao/CONTEXT_HANDOFF_PHASE4_PHASE5.md`
> - `AI_Agent/plans/Plan_Phase_4_Dashboard_Metrics_AWS_S3.md`
> - `AI_Agent/specs/api-spec.md` (Mục 14, 15, 16)
> - `AI_Agent/specs/backend-spec.md`
> - `src/scriptDB/init_schema.sql` (Bảng 15: `idempotency_keys`)
> - Codebase hiện tại: `shared-library`, `core-service`, `gateway-services`

---

## 1. Phân tích hiện trạng

### 1.1. Tình trạng Codebase sau Phase 3
1. **Module `shared-library`**:
   - Đã triển khai hoàn chỉnh: `AuditableEntity` (UUID v7), `ResponseCode` (hiện có 29 enums chuẩn), `MessageUtils`, `I18nConfig`, `CoreException`, `CorsConfig`, `CorsProperties`, `TechmartPreauthorize`, `PagedResponse`, `ErrorResponse`.
   - Các file i18n `messages.properties` và `messages_vi.properties` đã ánh xạ 29 mã lỗi.
   - Test suite: 8 tests PASS (`AuditableEntityTest`, `HeaderConstantsTest`, `ResponseCodeTest`, `DtoSerializationTest`, `CoreExceptionTest`, `I18nConfigTest`, `MessageUtilsTest`, `CorsConfigTest`).
   - **Lưu ý đặc biệt**: `ResponseCodeTest.java` có assertion cứng kiểm tra đúng 29 enums (`assertEquals(29, ResponseCode.values().length)`). Khi bổ sung 5 enums mới trong Phase 4, cần cập nhật assertion lên 34 và bổ sung đầy đủ i18n để bảo đảm 100% tests PASS!

2. **Module `gateway-services`**:
   - Hoạt động như Public API Gateway (Port 8080), không kết nối DB.
   - Đã có JWT filter, authentication, CORS, rate limiting.
   - `CoreClient`: Sử dụng Spring `RestClient`, chuyển tiếp request sang Core (Port 8081) kèm `X-Internal-Secret`, `Correlation-Id`, `X-User-Id`, `X-User-Role`, `X-User-Email`.
   - **Thiếu sót hiện tại**:
     - `CoreClient` chưa chuyển tiếp header `Idempotency-Key` từ Client sang Core.
     - `CoreClient` chỉ hỗ trợ gửi JSON body, chưa hỗ trợ `MultipartFile` cho file upload hoặc `DELETE` endpoint cho storage.
     - Chưa có controller proxy cho `/api/v1/admin/metrics/**` và `/api/v1/files/**`.

3. **Module `core-service`**:
   - Triển khai theo mô hình CQRS Layer-First (`controller/`, `bus/`, `handler/`, `service/command/`, `service/query/`, `repository/`, `entity/`).
   - CSDL 15 bảng theo `init_schema.sql`: 14 bảng đầu đã hoạt động với JPA Entity tương ứng; bảng thứ 15 `idempotency_keys` đã có DDL trong `init_schema.sql` nhưng chưa có JPA Entity và Repository trong Java.
   - `InternalOrderApiControllerImpl`: Đã có API tạo đơn `/internal/api/v1/orders` nhưng chưa được bọc `@Idempotent`.
   - Chưa có phân hệ `metrics` (DTO, Query, Handler, QueryService, Controller).
   - Chưa có tích hợp MinIO SDK (`io.minio:minio`) và `StorageService`.

4. **Kết quả kiểm thử hồi quy**:
   - Toàn bộ 106 tests hiện có (Phase 00 đến Phase 03) đang hoạt động ổn định trên H2 PostgreSQL-mode in-memory DB.

---

## 2. Phạm vi thay đổi

### 2.1. Module `shared-library`
#### Tạo mới
- [ ] `shared-library/src/main/java/com/techmart/shared/annotation/Idempotent.java` — Custom Annotation `@Idempotent(expireHours = 24)` áp dụng trên method tạo đơn và các giao dịch kho vận nhạy cảm để kích hoạt Spring AOP Idempotency aspect.

#### Chỉnh sửa
- [ ] `shared-library/src/main/java/com/techmart/shared/constant/HeaderConstants.java` — Bổ sung hằng số `public static final String IDEMPOTENCY_KEY = "Idempotency-Key";`.
- [ ] `shared-library/src/main/java/com/techmart/shared/constant/ResponseCode.java` — Bổ sung 5 mã lỗi:
  - `IDEMPOTENCY_KEY_IN_PROGRESS` (`HttpStatus.CONFLICT`, `"error.idempotency_key_in_progress"`)
  - `IDEMPOTENCY_KEY_PAYLOAD_MISMATCH` (`HttpStatus.UNPROCESSABLE_ENTITY`, `"error.idempotency_key_payload_mismatch"`)
  - `FILE_STORAGE_ERROR` (`HttpStatus.INTERNAL_SERVER_ERROR`, `"error.file_storage_error"`)
  - `INVALID_FILE_TYPE` (`HttpStatus.BAD_REQUEST`, `"error.invalid_file_type"`)
  - `FILE_TOO_LARGE` (`HttpStatus.BAD_REQUEST`, `"error.file_too_large"`)
- [ ] `shared-library/src/main/resources/messages/messages.properties` — Bổ sung thông điệp i18n tiếng Anh cho 5 mã lỗi mới.
- [ ] `shared-library/src/main/resources/messages/messages_vi.properties` — Bổ sung thông điệp i18n tiếng Việt cho 5 mã lỗi mới.
- [ ] `shared-library/src/test/java/com/techmart/shared/constant/ResponseCodeTest.java` — Cập nhật kiểm tra số lượng enum từ 29 lên 34 để test tiếp tục PASS 100%.
- [ ] `shared-library/src/test/java/com/techmart/shared/constant/HeaderConstantsTest.java` — Bổ sung kiểm tra hằng số `IDEMPOTENCY_KEY`.

---

### 2.2. Root `pom.xml`
#### Chỉnh sửa
- [ ] `pom.xml` — Bổ sung property `<minio.version>8.5.17</minio.version>` và khai báo dependency `io.minio:minio` trong `<dependencyManagement>`.

---

### 2.3. Module `core-service`
#### Chỉnh sửa
- [ ] `core-service/pom.xml` — Bổ sung dependency `io.minio:minio`.
- [ ] `core-service/src/main/resources/application.yml` — Bổ sung cấu hình `techmart.minio` (endpoint, access-key, secret-key, 4 bucket names: products, brands, avatars, banners).
- [ ] `core-service/src/test/resources/application.yml` — Bổ sung cấu hình mock/test an toàn cho `techmart.minio`.
- [ ] `core-service/src/main/java/com/techmart/core/controller/order/impl/InternalOrderApiControllerImpl.java` — Gắn annotation `@Idempotent(expireHours = 24)` lên method `createOrder(...)`.

#### Tạo mới: Phân hệ Idempotency Engine
- [ ] `core-service/src/main/java/com/techmart/core/entity/idempotency/IdempotencyStatus.java` — Enum trạng thái vòng đời idempotency: `IN_PROGRESS`, `COMPLETED`, `FAILED`.
- [ ] `core-service/src/main/java/com/techmart/core/entity/idempotency/IdempotencyKeyEntity.java` — JPA Entity ánh xạ bảng `idempotency_keys` (`id`, `idempotency_key`, `user_id`, `request_path`, `request_hash`, `status`, `response_status`, `response_body`, `created_at`, `expires_at`).
- [ ] `core-service/src/main/java/com/techmart/core/repository/idempotency/IdempotencyKeyRepository.java` — Spring Data JPA Repository cho bảng `idempotency_keys` với query `findByIdempotencyKeyAndUserId`.
- [ ] `core-service/src/main/java/com/techmart/core/service/command/idempotency/IdempotencyService.java` — Interface quản lý các thao tác ghi trạng thái idempotency với ranh giới giao dịch riêng biệt (`REQUIRES_NEW`).
- [ ] `core-service/src/main/java/com/techmart/core/service/command/idempotency/impl/IdempotencyServiceImpl.java` — Triển khai `IdempotencyService`: insert `IN_PROGRESS`, update `COMPLETED` + snapshot response body JSON, update `FAILED`, kiểm tra hash SHA-256, đếm blocked requests.
- [ ] `core-service/src/main/java/com/techmart/core/aspect/IdempotencyAspect.java` — Spring AOP `@Around("@annotation(idempotent)")`: Đọc header `Idempotency-Key`, kiểm tra trùng lặp, replay snapshot cached response nếu đã `COMPLETED`, ném 409 Conflict nếu `IN_PROGRESS`, ném 422 Unprocessable Entity nếu sửa đổi request body.

#### Tạo mới: Phân hệ Admin Metrics & Monitoring (CQRS Layer-First)
- [ ] `core-service/src/main/java/com/techmart/core/dto/response/metrics/DashboardSummaryResponse.java` — DTO phong bì KPI: `revenue`, `orders`, `inventory`, `customers`, `calculated_at`.
- [ ] `core-service/src/main/java/com/techmart/core/dto/response/metrics/RevenueChartResponse.java` — DTO biểu đồ doanh thu: `period`, `timeline`, `total_period_revenue`, `total_period_orders`.
- [ ] `core-service/src/main/java/com/techmart/core/dto/response/metrics/StockAlertItemResponse.java` — DTO biến thể cảnh báo tồn kho: `branch_id`, `branch_name`, `product_id`, `product_name`, `variant_id`, `sku`, `attributes`, `quantity_on_hand`, `quantity_reserved`, `quantity_available`, `reorder_level`, `alert_status`.
- [ ] `core-service/src/main/java/com/techmart/core/dto/response/metrics/SystemHealthResponse.java` — DTO sức khỏe hệ thống: `server`, `memory`, `database_pool`, `cache`, `idempotency_metrics`.
- [ ] `core-service/src/main/java/com/techmart/core/bus/query/metrics/GetDashboardSummaryQuery.java` — Query SpringBus lấy KPI summary (kèm optional `branchId`).
- [ ] `core-service/src/main/java/com/techmart/core/bus/query/metrics/GetRevenueChartQuery.java` — Query SpringBus lấy biểu đồ doanh thu (kèm `period`, optional `branchId`).
- [ ] `core-service/src/main/java/com/techmart/core/bus/query/metrics/GetStockAlertsQuery.java` — Query SpringBus lấy cảnh báo tồn (kèm optional `branchId`, `threshold`, `Pageable`).
- [ ] `core-service/src/main/java/com/techmart/core/bus/query/metrics/GetSystemHealthQuery.java` — Query SpringBus lấy sức khỏe hệ thống.
- [ ] `core-service/src/main/java/com/techmart/core/handler/metrics/MetricsHandler.java` — SpringBus Query Handler cho 4 Query trên.
- [ ] `core-service/src/main/java/com/techmart/core/service/query/metrics/MetricsQueryService.java` — Interface truy vấn đo lường.
- [ ] `core-service/src/main/java/com/techmart/core/service/query/metrics/impl/MetricsQueryServiceImpl.java` — Triển khai `MetricsQueryService` (`@Transactional(readOnly = true)`): Single-Query SQL Aggregation `FILTER (WHERE ...)` < 5ms, gom nhóm timeline chuỗi thời gian kèm zero-filling, đọc JVM MXBeans và HikariCP pool.
- [ ] `core-service/src/main/java/com/techmart/core/controller/metrics/InternalMetricsApiController.java` — Interface nội bộ cho 4 API metrics.
- [ ] `core-service/src/main/java/com/techmart/core/controller/metrics/impl/InternalMetricsApiControllerImpl.java` — Controller nội bộ `/internal/api/v1/metrics/**`.

#### Tạo mới: Phân hệ MinIO Object Storage
- [ ] `core-service/src/main/java/com/techmart/core/config/MinioConfig.java` — Cấu hình `MinioClient` Bean từ các biến môi trường và thiết lập các bucket properties.
- [ ] `core-service/src/main/java/com/techmart/core/dto/response/storage/FileUploadResponse.java` — DTO kết quả upload tệp tin: `bucket`, `file_url`, `file_name`, `file_size`, `content_type`.
- [ ] `core-service/src/main/java/com/techmart/core/service/storage/StorageService.java` — Interface quản lý lưu trữ tệp tin: `uploadFile(MultipartFile file, String bucketType)`, `deleteFile(String fileName, String bucketType)`.
- [ ] `core-service/src/main/java/com/techmart/core/service/storage/impl/MinioStorageServiceImpl.java` — Triển khai lưu trữ MinIO: Tự động kiểm tra và khởi tạo 4 buckets (`techmart-products`, `techmart-brands`, `techmart-avatars`, `techmart-banners`), áp dụng anonymous download policy, kiểm tra kích thước $\le 5\text{MB}$, kiểm tra MIME types (JPEG, PNG, WEBP), sinh UUID v7 cho tên file, upload và xóa file.
- [ ] `core-service/src/main/java/com/techmart/core/controller/storage/InternalStorageApiController.java` — Interface Controller nội bộ lưu trữ tệp tin.
- [ ] `core-service/src/main/java/com/techmart/core/controller/storage/impl/InternalStorageApiControllerImpl.java` — Controller nội bộ `/internal/api/v1/files/**` nhận Multipart upload và DELETE request.

---

### 2.4. Module `gateway-services`
#### Chỉnh sửa
- [ ] `gateway-services/src/main/java/com/techmart/gateway/client/CoreClient.java` — Chuyển tiếp header `Idempotency-Key` từ request của Client sang Core Service; bổ sung hỗ trợ `forwardMultipart` và `forward` với HttpMethod `DELETE`.
- [ ] `gateway-services/src/main/java/com/techmart/gateway/controller/order/GatewayOrderController.java` — Tiếp nhận và đảm bảo chuyển tiếp header `Idempotency-Key` khi proxy `POST /api/v1/orders`.
- [ ] `gateway-services/src/main/java/com/techmart/gateway/security/GatewaySecurityConfig.java` — Cấu hình phân quyền URL:
  - `/api/v1/admin/metrics/system-health`: `ADMIN` only
  - `/api/v1/admin/metrics/**`: `ADMIN`, `STAFF`
  - `/api/v1/files/upload`: `ADMIN`, `STAFF`
  - `DELETE /api/v1/files/**`: `ADMIN` only

#### Tạo mới
- [ ] `gateway-services/src/main/java/com/techmart/gateway/controller/admin/GatewayMetricsController.java` — Controller proxy cho 4 endpoint `/api/v1/admin/metrics/*` (`summary`, `revenue-chart`, `stock-alerts`, `system-health`).
- [ ] `gateway-services/src/main/java/com/techmart/gateway/controller/storage/GatewayFileController.java` — Controller proxy cho `/api/v1/files/upload` (Multipart) và `/api/v1/files/{fileName}` (DELETE).

---

### 2.5. Kiểm thử Tích hợp (Test Suites)
#### Tạo mới
- [ ] `core-service/src/test/java/com/techmart/core/IdempotencyEngineCoreTests.java` — Kiểm thử toàn diện Idempotency: tạo đơn thành công lần đầu, concurrent conflict 409, replay snapshot khi body khớp hash, từ chối 422 khi payload mismatch, khôi phục khi gặp lỗi FAILED.
- [ ] `core-service/src/test/java/com/techmart/core/AdminMetricsCoreTests.java` — Kiểm thử 4 query metrics: Single-Query Aggregation KPI, timeline biểu đồ có zero-fill, cảnh báo tồn kho phân trang, system health real-time và kiểm soát quyền.
- [ ] `core-service/src/test/java/com/techmart/core/MinioStorageCoreTests.java` — Kiểm thử MinIO (sử dụng Mock MinioClient): auto-create bucket, upload thành công ảnh với UUID v7, từ chối file > 5MB hoặc sai MIME type (400), xóa file thành công (204).
- [ ] `gateway-services/src/test/java/com/techmart/gateway/GatewayMetricsAndStorageSecurityTests.java` — Kiểm thử phân quyền RBAC và Zero-Trust trên Gateway cho các endpoint metrics và storage mới.

---

## 3. Business Logic chi tiết

### 3.1. Phân hệ Idempotency Engine (Spring AOP Aspect)
```
                                 [ Client Request ]
                                         │
                             Header: Idempotency-Key
                                         ▼
                            [ Gateway: CoreClient ]
                                         │
                             Forward Idempotency-Key
                                         ▼
                        [ Core: IdempotencyAspect ]
                                         │
                      Có Header Idempotency-Key không?
                                 ├── KHÔNG ──► [ Cho qua: proceed() ]
                                 │
                                 └── CÓ
                                      ▼
                      Tính SHA-256 Hash của Request Body
                                      ▼
                        Tra cứu bảng idempotency_keys
                         (idempotency_key, user_id)
                                      │
            ┌─────────────────────────┼─────────────────────────┐
            ▼                         ▼                         ▼
      [ 1. Chưa có ]           [ 2. IN_PROGRESS ]        [ 3. COMPLETED ]
            │                         │                         │
  Insert IN_PROGRESS                  ▼                         ▼
  (REQUIRES_NEW Tx)            Trả 409 Conflict         So sánh SHA-256 Hash
            │              ("Giao dịch đang xử lý")             │
            ▼                                           ┌───────┴───────┐
    Gọi proceed()                                       ▼               ▼
 (OrderCommandService)                               [ Khớp ]      [ Không khớp ]
            │                                           │               │
  ┌─────────┴─────────┐                                 ▼               ▼
  ▼                   ▼                        Trả Snapshot Response   Trả 422
Thành công          Thất bại                    từ DB (HTTP 201)    Unprocessable
  │                   │                     (KHÔNG GỌI SERVICE,     Entity
  ▼                   ▼                     KHÔNG TRỪ KHO LẦN 2)
Cập nhật            Cập nhật
COMPLETED +         FAILED /
Snapshot JSON       Xóa key
(HTTP 201)
```

1. **Trích xuất thông tin định danh:**
   - Aspect bắt request tại Controller. Đọc `Idempotency-Key` từ header HTTP.
   - Nếu header vắng mặt hoặc rỗng: Bỏ qua kiểm tra, gọi trực tiếp `joinPoint.proceed()`.
   - Nếu có header: Lấy `userId` từ `UserContext.getCurrentUser()`. Tính SHA-256 hash của request body (hoặc argument truyền vào controller) để sinh chuỗi Hex 64 ký tự `requestHash`.
2. **Kiểm tra trạng thái bản ghi trong DB:**
   - Tra cứu bản ghi trong `idempotency_keys` theo `(idempotency_key, user_id)`:
     - **Trường hợp 1 (Chưa tồn tại hoặc đã quá hạn 24 giờ):**
       - Khởi tạo transaction `REQUIRES_NEW` để insert bản ghi mới với `status = 'IN_PROGRESS'`, `expires_at = now + 24h`.
       - Nếu xảy ra tranh chấp dữ liệu đồng thời (`DataIntegrityViolationException` do Unique constraint trên `idempotency_key`): Ném ngay `CoreException(ResponseCode.IDEMPOTENCY_KEY_IN_PROGRESS)` (HTTP 409 Conflict).
       - Thực thi nghiệp vụ qua `joinPoint.proceed()`.
       - Khi nghiệp vụ thành công: Chuyển trạng thái bản ghi sang `COMPLETED`, lưu HTTP status (201) và snapshot JSON của response vào cột `response_body`.
       - Nếu nghiệp vụ ném lỗi: Đánh dấu bản ghi sang `FAILED` (hoặc xóa) để cho phép thử lại an toàn trong tương lai.
     - **Trường hợp 2 (Đang tồn tại với `status = 'IN_PROGRESS'`):**
       - Request trước đó vẫn đang chạy trong luồng khác. Trả về ngay lập tức HTTP 409 Conflict với thông điệp: "Giao dịch đang xử lý, vui lòng chờ".
     - **Trường hợp 3 (Đã tồn tại với `status = 'COMPLETED'`):**
       - Kiểm tra tính toàn vẹn của nội dung: So sánh `requestHash` hiện tại với `request_hash` đã lưu.
       - *Nếu khớp 100%:* Replay ngay lập tức kết quả đã lưu trong snapshot `response_body` kèm HTTP status đã lưu (201 Created). **TUYỆT ĐỐI KHÔNG GỌI LẠI SERVICE, KHÔNG TRỪ KHO LẦN 2!** Tăng biến đếm Idempotency blocked counter.
       - *Nếu không khớp (Client sửa payload nhưng dùng lại key cũ):* Ném ngay `CoreException(ResponseCode.IDEMPOTENCY_KEY_PAYLOAD_MISMATCH)` (HTTP 422 Unprocessable Entity).

---

### 3.2. Phân hệ Admin Metrics & Monitoring (CQRS Layer-First)

1. **API 1: `GET /api/v1/admin/metrics/summary` (KPI Dashboard Tổng hợp):**
   - **Tối ưu hóa SQL cực hạn:** Đẩy toàn bộ phép toán tính toán và phân loại xuống PostgreSQL thông qua **Single-Query Aggregation** với mệnh đề `FILTER (WHERE ...)` trên bảng `orders`, thực thi $< 5\text{ms}$:
     ```sql
     SELECT 
         COALESCE(SUM(total_amount) FILTER (WHERE created_at >= :todayStart AND created_at < :todayEnd AND status = 'DELIVERED'), 0) AS today_revenue,
         COALESCE(SUM(total_amount) FILTER (WHERE created_at >= :yesterdayStart AND created_at < :todayStart AND status = 'DELIVERED'), 0) AS yesterday_revenue,
         COUNT(*) FILTER (WHERE created_at >= :todayStart AND created_at < :todayEnd) AS today_orders_total,
         COUNT(*) FILTER (WHERE created_at >= :todayStart AND created_at < :todayEnd AND status = 'PENDING') AS pending_count,
         COUNT(*) FILTER (WHERE created_at >= :todayStart AND created_at < :todayEnd AND status = 'CONFIRMED') AS confirmed_count,
         COUNT(*) FILTER (WHERE created_at >= :todayStart AND created_at < :todayEnd AND status = 'PROCESSING') AS processing_count,
         COUNT(*) FILTER (WHERE created_at >= :todayStart AND created_at < :todayEnd AND status = 'SHIPPED') AS shipped_count,
         COUNT(*) FILTER (WHERE created_at >= :todayStart AND created_at < :todayEnd AND status = 'DELIVERED') AS delivered_count,
         COUNT(*) FILTER (WHERE created_at >= :todayStart AND created_at < :todayEnd AND status = 'CANCELLED') AS cancelled_count
     FROM orders
     WHERE (:branchId IS NULL OR branch_id = :branchId);
     ```
   - Tương tự với tồn kho và khách hàng:
     - Tồn kho: Đếm `low_stock_variants_count` (tồn khả dụng $\le \text{reorder\_level}$ và $> 0$) và `out_of_stock_variants_count` (tồn khả dụng $\le 0$).
     - Khách hàng: Đếm `new_customers_today` (`created_at >= todayStart` và `role = 'CUSTOMER'`) và `total_active_customers` (`deleted = false`).
   - Tỷ lệ tăng trưởng doanh thu: Tính toán theo công thức `((today - yesterday) / yesterday) * 100`, có xử lý biên chống chia cho 0.

2. **API 2: `GET /api/v1/admin/metrics/revenue-chart` (Biểu đồ chuỗi thời gian):**
   - Tham số: `period` (`TODAY`, `7_DAYS`, `30_DAYS`, `THIS_MONTH`, `THIS_YEAR`), mặc định `7_DAYS`; `branch_id` (optional).
   - Truy vấn SQL gom nhóm theo ngày (`DATE(created_at)`) loại trừ đơn `CANCELLED`.
   - **Zero-Filling Algorithm:** Khởi tạo danh sách các mốc thời gian đầy đủ cho khoảng thời gian requested. Với mỗi mốc không có đơn hàng phát sinh từ DB, tự động điền `revenue = 0.00` và `orders_count = 0` để đảm bảo đồ thị Frontend hiển thị mượt mà, không bị khuyết điểm.

3. **API 3: `GET /api/v1/admin/metrics/stock-alerts` (Cảnh báo tồn kho khả dụng):**
   - Tham số: `branch_id` (optional), `threshold` (default = 5), `page` (default = 0), `size` (default = 10).
   - Truy vấn kết hợp bảng `inventory`, `product_variants`, `products`, `branches`.
   - Điều kiện lọc: `(i.quantity_on_hand - i.quantity_reserved) <= :threshold OR (i.quantity_on_hand - i.quantity_reserved) <= i.reorder_level`.
   - Gán `alert_status`:
     - `OUT_OF_STOCK`: tồn khả dụng $\le 0$
     - `CRITICAL_LOW`: $0 < \text{tồn khả dụng} \le 2$
     - `LOW_STOCK`: tồn khả dụng $\le$ threshold hoặc reorder_level
   - Trả về đối tượng `PagedResponse<StockAlertItemResponse>` chuẩn phân trang.

4. **API 4: `GET /api/v1/admin/metrics/system-health` (Sức khỏe hệ thống Real-time):**
   - Phân quyền nghiêm ngặt: `ADMIN` only. `STAFF` và `CUSTOMER` nhận `403 Forbidden`.
   - Đọc trực tiếp không qua cache:
     - Uptime: Lấy từ `ManagementFactory.getRuntimeMXBean().getUptime()`, format thành chuỗi `uptime_human` (X ngày Y giờ Z phút).
     - JVM Heap: `ManagementFactory.getMemoryMXBean().getHeapMemoryUsage()` (used MB, max MB, %).
     - HikariCP Database Connection Pool: Lấy từ `HikariDataSource` pool bean (`active`, `idle`, `total`, `threads awaiting`).
     - Cache: Kiểm tra ping Redis (`PONG`).
     - Idempotency Metrics: Đọc biến đếm nguyên tử `total_blocked_requests`.

---

### 3.3. Phân hệ MinIO Object Storage (ADR-026)

1. **Kiến trúc Đa Bucket Độc lập:**
   - 4 Buckets chuyên biệt:
     - `techmart-products`: Ảnh sản phẩm cha, ảnh biến thể, gallery.
     - `techmart-brands`: Logo thương hiệu / nhãn hàng.
     - `techmart-avatars`: Ảnh đại diện người dùng.
     - `techmart-banners`: Ảnh banner sự kiện / quảng cáo.
2. **Khởi tạo tự động & Public Policy:**
   - Khi Service được gọi, tự động kiểm tra bucket qua `minioClient.bucketExists(...)`. Nếu chưa có -> gọi `minioClient.makeBucket(...)`.
   - Thiết lập chính sách anonymous download (Download Policy) công khai để người dùng bên ngoài tải được ảnh trực tiếp qua URL mà không cần presigned URL.
3. **Quy chuẩn Upload File:**
   - Kiểm tra kích thước: File $\le 5\text{MB}$ ($5 \times 1024 \times 1024$ bytes). Nếu vượt -> ném `CoreException(ResponseCode.FILE_TOO_LARGE)` (400 Bad Request).
   - Kiểm tra định dạng MIME: Chỉ cho phép `image/jpeg`, `image/png`, `image/webp`. Nếu sai -> ném `CoreException(ResponseCode.INVALID_FILE_TYPE)` (400 Bad Request).
   - Đặt tên file: Tên file luôn được sinh bằng **UUID v7** kết hợp extension gốc (ví dụ: `018f2345-6789-7abc-def0-img000000001.webp`), loại bỏ hoàn toàn tên file gốc từ client để tránh tấn công path traversal hoặc trùng tên.
   - URL trả về: Định dạng chuẩn `http://{endpoint}/{bucketName}/{fileName}`.
4. **Xóa File:**
   - Endpoint: `DELETE /api/v1/files/{fileName}?bucket=products` (`ADMIN` only).
   - Gọi `minioClient.removeObject(...)`, trả về `204 No Content`.

---

## 4. Edge Cases

### 4.1. Idempotency Engine Edge Cases
1. **Client không truyền header `Idempotency-Key`:** Aspect cho qua, thực thi bình thường như luồng đặt hàng tiêu chuẩn, không ghi vào bảng `idempotency_keys`.
2. **Request Body rỗng hoặc null:** Hash SHA-256 được tính trên chuỗi rỗng `""` an toàn, không ném NullPointerException.
3. **Tranh chấp đồng thời chính xác tới mili-giây (Race Condition):** 2 luồng cùng thực thi insert `idempotency_key` đồng thời. Database constraint UNIQUE bắt ngoại lệ `DataIntegrityViolationException`, Aspect chuyển đổi thành `409 Conflict` lập tức, bảo đảm chỉ duy nhất 1 luồng được xử lý.
4. **Nghiệp vụ tạo đơn thất bại giữa chừng (ví dụ: hết hàng trong kho):** Khi ngoại lệ nghiệp vụ xảy ra, transaction tạo đơn rollback. Bản ghi idempotency được cập nhật sang `status = 'FAILED'` (hoặc xóa) để Client có thể sửa giỏ hàng và gửi lại đơn mới an toàn.
5. **Key đã hết hạn 24 giờ:** Nếu `expires_at < now`, khóa cũ được xem như hết hiệu lực, cho phép khởi tạo một giao dịch mới với key này.
6. **Replay response serialization:** Response body lưu trữ dưới dạng JSON snapshot chuẩn xác, khi đọc lại tái tạo đúng `ResponseEntity` với mã 201 Created và payload tương ứng.

### 4.2. Admin Metrics Edge Cases
1. **Chia cho 0 khi tính tỷ lệ tăng trưởng doanh thu:**
   - Doanh thu hôm qua = 0 và hôm nay = 0 $\implies$ Tăng trưởng = `0.0%`.
   - Doanh thu hôm qua = 0 và hôm nay > 0 $\implies$ Tăng trưởng = `100.0%`.
   - Doanh thu hôm qua > 0 $\implies$ Tính đúng công thức và làm tròn 2 chữ số thập phân.
2. **Khoảng thời gian trong biểu đồ không có dữ liệu:** Thuật toán zero-filling duyệt qua toàn bộ các ngày từ `startDate` đến `endDate`, điền 0 cho các ngày không có giao dịch, tránh làm đứt gãy biểu đồ trên giao diện UI.
3. **Lọc theo chi nhánh (`branch_id`) không có đơn hàng:** Trả về số liệu 0 đầy đủ cấu trúc, không ném NullPointerException hoặc trả về null.
4. **HikariCP pool không khởi tạo MXBean (môi trường kiểm thử H2):** Fallback an toàn, trả về giá trị mặc định 0 thay vì ném NullPointerException.

### 4.3. MinIO Storage Edge Cases
1. **Tham số `bucket` truyền sai tên:** Fallback mặc định về bucket `techmart-products` hoặc kiểm tra whitelist chặt chẽ.
2. **File rỗng (size = 0 bytes):** Bị từ chối ngay từ tầng validation với mã `VALIDATION_FAILED` (400 Bad Request).
3. **File đổi đuôi giả mạo (ví dụ: script độc hại đổi đuôi thành .jpg):** Kiểm tra cả Content-Type thực tế và phần mở rộng của file.
4. **MinIO Server bị ngắt kết nối tạm thời:** Bắt `MinioException` và đóng gói thành `CoreException(ResponseCode.FILE_STORAGE_ERROR)` (HTTP 500), ghi log chi tiết lỗi kết nối.

---

## 5. Tiêu chí nghiệm thu (16 ACs Checklist)

- [ ] **AC-01:** Tạo bảng `idempotency_keys` trong CSDL với khóa chính UUID v7, ràng buộc duy nhất trên `idempotency_key`, khóa ngoại `user_id`, check constraint `status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED')`, và 2 chỉ mục tìm kiếm `idx_idempotency_lookup(idempotency_key, user_id)` và `idx_idempotency_expiry(expires_at)`.
- [ ] **AC-02:** Xây dựng Custom Annotation `@Idempotent(expireHours = 24)` và Spring AOP `IdempotencyAspect` hoạt động theo cơ chế Non-Invasive trên API tạo đơn hàng `POST /orders` (`InternalOrderApiControllerImpl.createOrder(...)`).
- [ ] **AC-03:** Kiểm thử Idempotency gửi lần đầu thành công trả `201 Created`, đơn hàng được tạo, tồn kho được giữ chỗ, và lưu `status = COMPLETED` kèm snapshot JSON `response_body` vào bảng `idempotency_keys`.
- [ ] **AC-04:** Kiểm thử Idempotency gửi đồng thời 2 request cùng key khi đang `IN_PROGRESS` $\to$ request sau nhận ngay phản hồi `409 Conflict` ("Giao dịch đang xử lý, vui lòng chờ").
- [ ] **AC-05:** Kiểm thử Idempotency gửi lại request cùng key khi đã `COMPLETED` và body khớp SHA-256 hash $\to$ trả về ngay cached response snapshot cũ (`201 Created`), KHÔNG gọi lại Service, KHÔNG trừ kho lần 2!
- [ ] **AC-06:** Kiểm thử Idempotency gửi lại request cùng key nhưng sửa đổi body (khác SHA-256 hash) $\to$ bị từ chối với `422 Unprocessable Entity` ("Khóa idempotency này đã dùng cho nội dung đơn hàng khác").
- [ ] **AC-07:** API `GET /api/v1/admin/metrics/summary` tính toán chính xác doanh thu hôm nay, hôm qua, tăng trưởng %, và gom đủ số lượng đơn cho từng trạng thái (`PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`), tồn thấp/hết hàng, và khách hàng mới thông qua Single-Query Aggregation với `FILTER (WHERE ...)` trên PostgreSQL, thực thi $< 5\text{ms}$.
- [ ] **AC-08:** API `GET /api/v1/admin/metrics/revenue-chart` trả về mảng timeline theo chuỗi thời gian chính xác theo tham số `period` (`TODAY`, `7_DAYS`, `30_DAYS`, `THIS_MONTH`, `THIS_YEAR`), hỗ trợ lọc `branch_id`, tự động zero-fill các mốc thời gian không có doanh thu.
- [ ] **AC-09:** API `GET /api/v1/admin/metrics/stock-alerts` lọc chính xác các biến thể có tồn khả dụng $\le \text{threshold}$ hoặc $\le \text{reorder\_level}$, hỗ trợ lọc `branch_id`, gán đúng `alert_status` (`OUT_OF_STOCK`, `CRITICAL_LOW`, `LOW_STOCK`), phân trang `PagedResponse`.
- [ ] **AC-10:** API `GET /api/v1/admin/metrics/system-health` trả về số đo Uptime, JVM Heap Memory, HikariCP pool, Redis status và Idempotency blocked count; phân quyền nghiêm ngặt `ADMIN` only (chặn `STAFF` và `CUSTOMER` truy cập với `403 Forbidden`).
- [ ] **AC-11:** Tích hợp MinIO Client SDK `io.minio:minio` (8.5.x+) tự động kiểm tra và khởi tạo 4 buckets (`techmart-products`, `techmart-brands`, `techmart-avatars`, `techmart-banners`) nếu chưa tồn tại, kèm chính sách anonymous download công khai.
- [ ] **AC-12:** Upload ảnh thành công lên MinIO Bucket chỉ định (`POST /api/v1/files/upload?bucket=products`), tên file sinh ngẫu nhiên bằng UUID v7, trả về URL hợp lệ (`200 OK`).
- [ ] **AC-13:** Chặn upload file vượt quá 5MB (`FILE_TOO_LARGE`) hoặc sai định dạng MIME không phải JPEG/PNG/WEBP (`INVALID_FILE_TYPE`) với mã lỗi `400 Bad Request`.
- [ ] **AC-14:** Xóa file trên MinIO Bucket thành công trả `204 No Content` (`DELETE /api/v1/files/{fileName}?bucket=products`), phân quyền `ADMIN` only.
- [ ] **AC-15:** Ranh giới Zero-Trust: Gateway chuyển tiếp an toàn kèm `X-Internal-Secret` và `Correlation-Id`, Core từ chối 100% request trực tiếp không có secret nội bộ (`403 Forbidden`).
- [ ] **AC-16:** Kiểm thử hồi quy: Toàn bộ 106 tests từ Phase 00, 01, 02, 03 tiếp tục PASS 100%.

---

## 6. Rules Compliance Checklist

- [ ] **Cấu trúc thư mục tuân thủ CQRS Layer-First Rules:** Phân chia nghiêm ngặt `dto/`, `controller/`, `handler/`, `service/command/`, `service/query/`, `repository/`, `entity/`, `aspect/`.
- [ ] **Naming convention đúng chuẩn:** Request kết thúc bằng `Request`, Response kết thúc bằng `Response`, Entity kết thúc bằng `Entity`, Repository kết thúc bằng `Repository`, Service kết thúc bằng `Service`/`ServiceImpl`.
- [ ] **SRP — mỗi file/class một trách nhiệm:** `IdempotencyAspect` chỉ quản lý AOP idempotency, `MetricsQueryServiceImpl` chỉ xử lý đọc số liệu thống kê, `MinioStorageServiceImpl` chỉ xử lý upload/xóa file trên MinIO.
- [ ] **Không hardcode credentials/tokens:** Sử dụng placeholder biến môi trường `${ENV_VAR:default_safe}` cho toàn bộ mật khẩu DB, JWT, MinIO, Redis.
- [ ] **Không sử dụng FQN inline:** Toàn bộ import được đặt ở đầu file, không dùng package inline trong thân code.
- [ ] **Logging chuẩn `[ClassName][MethodName]` với placeholder `{}`:** Sử dụng SLF4J, tuyệt đối không cộng chuỗi `+` trong lệnh log.
- [ ] **Kiểm thử hồi quy bảo đảm 106 tests cũ tiếp tục PASS 100%:** Cập nhật assertion enum trong `ResponseCodeTest` tương thích với 34 enums và bảo đảm toàn bộ test suites cũ không bị ảnh hưởng.
