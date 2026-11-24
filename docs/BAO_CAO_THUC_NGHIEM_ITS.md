# Kiến trúc và Kết quả Thực nghiệm ITS-Shared-Mobility

Tài liệu tổng hợp kiến trúc kỹ thuật, kiểm soát bảo mật, tối ưu truy vấn và kết quả kiểm thử tự động & E2E của **ITS-Shared-Mobility**.

---

## 1. Kiến trúc 3 tầng & Các Endpoint API Cốt lõi

### 1.1. Xác thực, Phân quyền & Bảo mật (`backend/`)
- `POST /api/v1/auth/register`: Đăng ký tài khoản khách thuê mới, băm mật khẩu BCrypt, luôn gán quyền `USER` ở phía server và cấp JWT.
- `POST /api/v1/auth/login`: Đăng nhập và cấp Bearer JWT token.
- `GET /api/v1/auth/me`: Lấy thông tin phiên đăng nhập hiện tại.
- **Bảo mật luồng SSE Realtime**: Các endpoint SSE (`/api/v1/iot/stream`, `/api/v1/stations/availability-stream`, `/api/v1/stations/{id}/availability-stream`) yêu cầu xác thực JWT (qua header `Authorization` hoặc tham số `?token=` cho `EventSource`) khi `app.security.require-auth=true`, đồng thời gửi nhịp tim `: keepalive` mỗi **25 giây** qua `SseEmitterManager`.
- **Quản lý bí mật & cấu hình**: Loại bỏ toàn bộ API key VietMap / Maps / VNPay `hash-secret` cứng khỏi mã nguồn và Gradle; cung cấp các mẫu `.env.example`, `local.properties.example`.

### 1.2. Nghiệp vụ Shared Mobility & Cấu hình động (`/api/v1/mobility`, `/api/v1/configurations`)
- `GET /api/v1/mobility/stations`: Danh sách trạm xe kèm số dock khả dụng (truy vấn gộp `GROUP BY`, không phát sinh N+1).
- `GET /api/v1/mobility/vehicles/nearby?lat=...&lng=...&radiusKm=...`: Tìm xe lân cận theo công thức **Haversine**; đọc bán kính mặc định (`SEARCH_RADIUS_KM`) và chính sách giá hiện hành theo batch.
- `POST /api/v1/mobility/reserve`: Đặt giữ xe với **Pessimistic Write Lock** (`SELECT ... WITH (UPDLOCK, ROWLOCK)`), thời hạn giữ lấy trực tiếp từ `SystemConfig` (`HOLD_DURATION_MINUTES`).
- `POST /api/v1/mobility/rides/start`: Quét mã QR (`vehicleCode`) mở khóa xe và khởi tạo `RideSession`.
- `POST /api/v1/mobility/rides/{sessionId}/end`: Khóa xe, gán trạm trả gần nhất và tính tổng cước phí theo `PricingPolicy` kết hợp hệ số cước quãng đường (`DISTANCE_RATE_PER_KM`) từ `SystemConfig`.
- `PUT /api/v1/configurations/{id}`: Cập nhật tham số hệ thống với kiểm tra ràng buộc miền giá trị cho từng key (`HOLD_DURATION_MINUTES`, `LOW_BATTERY_THRESHOLD`, `DISTANCE_RATE_PER_KM`, `SEARCH_RADIUS_KM`) — trả về `400 Bad Request` nếu vượt ngưỡng hợp lệ và `404 Not Found` nếu không tồn tại.

### 1.3. Giải pháp Mô phỏng IoT & Báo cáo (`/api/v1/iot`, `/api/v1/reports`)
- `POST /api/v1/iot/telemetry`: Tiếp nhận bản tin telemetry GPS/pin/trạng thái khóa, áp dụng ngưỡng cảnh báo pin yếu động (`LOW_BATTERY_THRESHOLD`) và phát sự kiện SSE `fleet-telemetry`.
- `POST /api/v1/iot/simulate-step/{sessionId}`: Giả lập di chuyển GPS và cập nhật pin.
- `GET /api/v1/reports/*`: Thống kê doanh thu, lưu lượng chuyến đi và công suất trạm bằng truy vấn SQL có chặn khoảng thời gian half-open `[startOfDay, nextDayStartOfDay)` và loại trừ bút toán `HISTORICAL_ADJUSTMENT`.
- **JPA Session Boundary**: Tắt `spring.jpa.open-in-view=false`; toàn bộ mapping DTO thực hiện trong ranh giới `@Transactional`.

---

## 2. Kết quả Thực nghiệm Concurrency Control (Chống Double-Booking)

- **Cơ chế**: `@Lock(LockModeType.PESSIMISTIC_WRITE)` trong `VehicleRepository.findByIdForUpdate(id)` kết hợp kiểm tra trạng thái nguyên tử bên trong `@Transactional`.
- **Kiểm thử JUnit 5 + Spring Boot Test (`ConcurrencyReservationTest`)**:
  - `ExecutorService` + `CountDownLatch` mô phỏng **50 luồng song song** đặt giữ cùng 1 xe trên Microsoft SQL Server 2022.
  - Kết quả xác nhận: chỉ **1 yêu cầu thành công**, **49 yêu cầu bị từ chối**, trạng thái cuối của xe trong DB là **`RESERVED`**.

---

## 3. Ma trận Kiểm thử Tích hợp & Hồi quy

| Nhóm kiểm thử | Bộ test / Công cụ | Phạm vi xác minh |
| :--- | :--- | :--- |
| **Database Migration & Context** | Flyway (`V1__init_schema.sql`) + `ItsSharedMobilityApplicationTests` + `DemoSeedDisabledTest` | Tạo mới schema từ DB rỗng sạch và khởi chạy khi `ITS_DEMO_SEED=false` không lỗi. |
| **Concurrency Lock** | `ConcurrencyReservationTest` | 50 luồng đồng thời đặt 1 xe -> đúng 1 thành công, 49 thất bại, xe ở trạng thái `RESERVED`. |
| **Ride Lifecycle & VNPay** | `RideLifecycleTest` | Giữ xe -> mở khóa QR -> mô phỏng IoT GPS -> kết thúc chuyến -> tính cước -> khởi tạo giao dịch VNPay. |
| **Security & RBAC Audit** | `SecurityAndWorkflowAuditTest` | Xác thực JWT, bảo vệ endpoint nhạy cảm, chặn thao tác sai quyền, kiểm tra ràng buộc `PricingPolicy` & `Feedback`. |
| **Post-Audit Regression** | `PostAuditImprovementsTest` | Kiểm tra `SystemConfig` có hiệu lực runtime (`HOLD_DURATION_MINUTES`, `LOW_BATTERY_THRESHOLD`, `DISTANCE_RATE_PER_KM`), validation `400`/`404`, SSE `401` khi thiếu token & `200` với `?token=`, biên thời gian báo cáo `23:59:59.500`, và tương thích `open-in-view=false`. |
| **Web Admin (`frontend/`)** | `npx tsc --noEmit`, `npm run lint`, `npm run build` + E2E trình duyệt | Next.js 16 `proxy.ts`, 0 lỗi/cảnh báo ESLint, Skeleton khi hydrate user, kết nối SSE có `?token=`. |
| **Mobile Android (`mobile-android/`)** | `./gradlew :app:testDebugUnitTest :app:assembleDebug` | Unit test parser QR & DTO models (`QrAndModelTest`), lưu phiên JWT `SessionManager`, tự đăng xuất khi HTTP `401`, fallback GPS & gửi báo cáo sự cố. |
| **CI/CD Pipeline** | `.github/workflows/ci.yml` | Tự động chạy `web`, `backend` (SQL Server 2022 container) và `android` trên push/PR. |
