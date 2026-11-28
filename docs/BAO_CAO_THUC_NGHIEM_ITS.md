# Kiến trúc và Kết quả Thực nghiệm ITS-Shared-Mobility

Tài liệu tổng hợp kiến trúc, bảo mật, kết quả kiểm thử tự động và minh chứng demo của **ITS-Shared-Mobility**.

---

## 1. Kiến trúc 3 tầng & Các Endpoint API Cốt lõi

### 1.1. Xác thực, Phân quyền & Bảo mật (`backend/`)
- `POST /api/v1/auth/register`: Đăng ký tài khoản khách thuê mới, băm mật khẩu BCrypt, luôn gán quyền `USER` ở phía server và cấp JWT.
- `POST /api/v1/auth/login`: Đăng nhập và cấp Bearer JWT token.
- `GET /api/v1/auth/me`: Lấy thông tin phiên đăng nhập hiện tại.
- **Xác thực SSE**: `/api/v1/iot/stream`, `/api/v1/stations/availability-stream` và `/api/v1/stations/{id}/availability-stream` yêu cầu JWT khi `APP_SECURITY_REQUIRE_AUTH=true` (mặc định). `EventSource` có thể truyền `?token=`. `SseEmitterManager` gửi tín hiệu giữ kết nối mỗi **25 giây**.
- **Quản lý bí mật & cấu hình**: Loại bỏ toàn bộ API key VietMap / Maps / VNPay `hash-secret` cứng khỏi mã nguồn và Gradle; cung cấp các mẫu `.env.example`, `local.properties.example`.

### 1.2. Nghiệp vụ Shared Mobility & Cấu hình động (`/api/v1/mobility`, `/api/v1/configurations`)
- `GET /api/v1/mobility/stations`: Danh sách trạm xe kèm số dock khả dụng (truy vấn gộp `GROUP BY`, không phát sinh N+1).
- `GET /api/v1/mobility/vehicles/nearby?lat=...&lng=...&radiusKm=...`: Tìm xe lân cận theo công thức **Haversine**; đọc bán kính mặc định (`SEARCH_RADIUS_KM`) và chính sách giá hiện hành theo batch.
- `POST /api/v1/mobility/reserve`: Đặt giữ xe với **Pessimistic Write Lock** (`SELECT ... WITH (UPDLOCK, ROWLOCK)`), thời hạn giữ lấy trực tiếp từ `SystemConfig` (`HOLD_DURATION_MINUTES`).
- `POST /api/v1/mobility/rides/start`: Quét mã QR (`vehicleCode`) mở khóa xe và khởi tạo `RideSession`.
- `POST /api/v1/mobility/rides/{sessionId}/end`: Khóa xe, gán trạm trả được chỉ định hoặc trạm gần nhất, tính cước và phát trạng thái xe mới qua SSE. Thanh toán diễn ra sau khi cước đã được tính. Xem [trình tự mở khóa, trả xe và thanh toán](diagrams/fig_4_11_ride_sequence.png).
- `PUT /api/v1/configurations/{id}`: Cập nhật tham số hệ thống với kiểm tra ràng buộc miền giá trị cho từng key (`HOLD_DURATION_MINUTES`, `LOW_BATTERY_THRESHOLD`, `DISTANCE_RATE_PER_KM`, `SEARCH_RADIUS_KM`) — trả về `400 Bad Request` nếu vượt ngưỡng hợp lệ và `404 Not Found` nếu không tồn tại.

### 1.3. Giải pháp Mô phỏng IoT & Báo cáo (`/api/v1/iot`, `/api/v1/reports`)
- `POST /api/v1/iot/telemetry`: Tiếp nhận bản tin telemetry GPS/pin/trạng thái khóa, áp dụng ngưỡng cảnh báo pin yếu động (`LOW_BATTERY_THRESHOLD`) và phát sự kiện SSE `fleet-telemetry`.
- `POST /api/v1/iot/simulate-step/{sessionId}`: Giả lập di chuyển GPS và cập nhật pin.
- `GET /api/v1/reports/*`: Thống kê doanh thu, lưu lượng chuyến đi và công suất trạm bằng truy vấn SQL có chặn khoảng thời gian half-open `[startOfDay, nextDayStartOfDay)` và loại trừ bút toán `HISTORICAL_ADJUSTMENT`.
- **Phiên JPA**: `spring.jpa.open-in-view=false` giới hạn việc đọc quan hệ JPA trong các phương thức dịch vụ có giao dịch.

---

## 2. Kết quả Thực nghiệm Concurrency Control (Chống Double-Booking)

- **Cơ chế**: `@Lock(LockModeType.PESSIMISTIC_WRITE)` trong `VehicleRepository.findByIdForUpdate(id)` kết hợp kiểm tra trạng thái nguyên tử bên trong `@Transactional`.
- **Kiểm thử JUnit 5 + Spring Boot Test (`ConcurrencyReservationTest`)**:
  - `ExecutorService` và `CountDownLatch` cho **50 luồng** gọi dịch vụ đặt giữ cùng một xe trên SQL Server 2022.
  - Test kiểm tra đúng **1 lượt giữ thành công**, **49 lượt bị từ chối** và xe ở trạng thái **`RESERVED`** trước khi hủy lượt giữ để dọn dữ liệu.
  - Test bật trình tạo dữ liệu mẫu. Chạy trên cơ sở dữ liệu kiểm thử dùng riêng.

---

## 3. Ma trận Kiểm thử Tích hợp & Hồi quy

| Nhóm kiểm thử | Bộ test / Công cụ | Phạm vi xác minh |
| :--- | :--- | :--- |
| **Lược đồ và dữ liệu mẫu** | `ItsSharedMobilityApplicationTests`, `DemoSeedDisabledTest` | Khởi tạo ứng dụng trên SQL Server dùng riêng và kiểm tra dữ liệu hiện có được giữ khi tắt trình tạo dữ liệu mẫu. |
| **Concurrency Lock** | `ConcurrencyReservationTest` | 50 luồng đồng thời đặt 1 xe -> đúng 1 thành công, 49 thất bại, xe ở trạng thái `RESERVED`. |
| **Vòng đời chuyến đi** | `RideLifecycleTest` | Bắt đầu và kết thúc chuyến, chặn thanh toán sớm, tính cước sau khi trả xe, thống kê chuyến qua ngày và quyết toán bằng tiền mặt. |
| **Xác thực và luồng nghiệp vụ** | `SecurityAndWorkflowAuditTest` | Kiểm tra thiếu JWT, phân quyền và quyền sở hữu, hết hạn giữ xe, gọi lại thao tác bắt đầu chuyến, chữ ký và IPN VNPay lặp, số liệu báo cáo. |
| **Post-Audit Regression** | `PostAuditImprovementsTest` | Kiểm tra `SystemConfig` có hiệu lực runtime (`HOLD_DURATION_MINUTES`, `LOW_BATTERY_THRESHOLD`, `DISTANCE_RATE_PER_KM`), validation `400`/`404`, SSE `401` khi thiếu token & `200` với `?token=`, biên thời gian báo cáo `23:59:59.500`, và tương thích `open-in-view=false`. |
| **Web (`frontend/`)** | `npx tsc --noEmit`, `npm run lint`, `npm run build` | Kiểm tra kiểu TypeScript, quy tắc mã nguồn và bản dựng Next.js. |
| **Android (`mobile-android/`)** | `./gradlew :app:testDebugUnitTest :app:assembleDebug` | Unit test phân tích mã QR và mô hình dữ liệu (`QrAndModelTest`), cùng bản dựng APK debug. |
| **CI/CD Pipeline** | `.github/workflows/ci.yml` | Tự động chạy `web`, `backend` (SQL Server 2022 container) và `android` trên push/PR. |

Ba job trên đều đạt trong [lần chạy CI của commit `88368f7`](https://github.com/tudptse17520/ITS-Shared-Mobility/actions/runs/45990857686). [Video demo Android](recordings/mobile_its_test.mp4) ghi lại phần thao tác trên ứng dụng.
