# Tài liệu Kỹ thuật & Kết quả Thực nghiệm Tiểu luận `Project_ITS`

Tài liệu này tổng hợp kiến trúc, giải pháp kỹ thuật và số liệu thực nghiệm đã chạy kiểm chứng trong dự án `Project_ITS`, phục vụ trực tiếp cho các **Chương 2, Chương 3 và Chương 4** trong đề cương tiểu luận.

---

## 1. Các Endpoint API Cốt lõi (Chương 3.1 & 3.4)

### 1.1. Xác thực & Tài khoản (`/api/v1/auth`)
- `POST /api/v1/auth/register`: Đăng ký tài khoản khách thuê mới, băm mật khẩu bằng BCrypt và cấp JWT.
- `POST /api/v1/auth/login`: Đăng nhập và cấp JWT token.
- `GET /api/v1/auth/me`: Lấy thông tin phiên đăng nhập hiện tại.

### 1.2. Nghiệp vụ Shared Mobility (`/api/v1/mobility`)
- `GET /api/v1/mobility/stations`: Danh sách trạm xe công cộng kèm tọa độ GPS và số xe khả dụng.
- `GET /api/v1/mobility/vehicles/nearby?lat=...&lng=...&radiusKm=...`: Tìm xe gần vị trí người dùng nhất theo công thức địa lý **Haversine**.
- `POST /api/v1/mobility/reserve`: Đặt giữ xe trước 15 phút (áp dụng **Pessimistic Write Lock** trên bảng `vehicle`).
- `POST /api/v1/mobility/rides/start`: Quét mã QR (`vehicleCode`) mở khóa xe và khởi tạo chuyến đi (`ParkingSession`).
- `POST /api/v1/mobility/rides/{sessionId}/end`: Khóa xe, kết thúc chuyến đi, gán trạm gần nhất và tính tổng cước phí theo `PricingPolicy` (Phí mở khóa + Cước theo thời gian + Phụ phí quãng đường).

### 1.3. Giải pháp Mô phỏng IoT trong ITS (`/api/v1/iot`)
- `POST /api/v1/iot/telemetry`: Tiếp nhận bản tin telemetry giả lập (`latitude`, `longitude`, `batteryLevel`, `speedKmh`, `lockStatus`), lưu vào `vehicle_telemetry_log`, cộng dồn quãng đường Haversine và phát sự kiện realtime.
- `POST /api/v1/iot/simulate-step/{sessionId}`: Giả lập 1 bước xe di chuyển thực tế (~250–400m) và tiêu hao 1–2% pin.
- `POST /api/v1/iot/vehicles/{vehicleId}/control`: Điều khiển từ xa (`UNLOCK`, `LOCK`, `RECHARGE_100`, `MAINTENANCE`, `SET_AVAILABLE`).
- `GET /api/v1/iot/stream`: Kênh **Server-Sent Events (SSE)** đẩy cập nhật vị trí GPS và dung lượng pin về Web Admin theo thời gian thực.

---

## 2. Kết quả Thực nghiệm Concurrency Control (Chương 4 & 5)

### Mục tiêu kiểm thử
Đánh giá tính toàn vẹn dữ liệu khi có nhiều người dùng đồng thời (Concurrent Users) cùng cạnh tranh đặt thuê cùng một chiếc xe (`ITS-EB-001`), đảm bảo không xảy ra hiện tượng **Double Booking**.

### Cơ chế xử lý
- Sử dụng `@Lock(LockModeType.PESSIMISTIC_WRITE)` trong `VehicleRepository.findByIdForUpdate(id)` (tương ứng `SELECT ... WITH (UPDLOCK, ROWLOCK)` trên Microsoft SQL Server) kết hợp kiểm tra trạng thái nguyên tử bên trong `@Transactional`.

### Số liệu chạy thực tế (`ConcurrencyReservationTest` & HTTP Load Test)
- **Kịch bản 1 — JUnit 5 + Spring Boot Test (`ConcurrencyReservationTest`)**:
  - **Công cụ**: `ExecutorService` + `CountDownLatch` 50 luồng song song.
  - **Đối tượng kiểm thử**: Phương tiện `ITS-EB-001` (`ID = 17`).
  - **Kết quả đo được**:
    - Tổng số yêu cầu gửi đồng thời: **50 requests**
    - Số yêu cầu đặt giữ thành công (`200 OK`): **1 request**
    - Số yêu cầu bị từ chối an toàn (`409 Conflict`): **49 requests**
    - Trạng thái cuối cùng của xe trong CSDL: **`RESERVED`** (toàn vẹn tuyệt đối)
    - Thời gian xử lý toàn bộ 50 luồng đồng thời: **134 ms**
    - Độ trễ của yêu cầu thành công đầu tiên: **74,00 ms**
- **Kịch bản 2 — Kiểm thử HTTP 20 Request đồng thời qua TCP (`POST /api/v1/mobility/reserve`)**:
  - **Đối tượng kiểm thử**: Phương tiện `ITS-EB-001` (`ID = 33`).
  - **Kết quả đo được**:
    - Tổng số yêu cầu HTTP gửi đồng thời: **20 requests**
    - Số yêu cầu thành công (`HTTP 200 OK`): **1 request**
    - Số yêu cầu bị chặn (`HTTP 409 Conflict`): **19 requests** (không phát sinh lỗi `500`)
    - Tổng thời gian hoàn tất 20 HTTP requests: **79 ms**

---

## 3. Bảng Tổng hợp Kết quả Kiểm thử Tích hợp End-to-End (E2E Test Matrix)

| Mã TC | Nhóm tính năng | Endpoint / Thành phần kiểm thử | Kết quả thực tế ghi nhận | Đánh giá |
| :--- | :--- | :--- | :--- | :---: |
| **TC-01** | Xác thực & Phân quyền JWT | `POST /api/v1/auth/login`, `POST /api/v1/auth/register`, `GET /api/v1/auth/me` | Đăng nhập `admin` (`Role=ADMIN`), đăng ký tài khoản mới (`Role=USER`), truy xuất hồ sơ qua Bearer JWT thành công (`200 OK`). | **PASS** |
| **TC-02** | Trạm xe & Tìm xe gần nhất (Haversine) | `GET /api/v1/mobility/stations`, `GET /api/v1/mobility/vehicles/nearby` | Trả về đủ **6 trạm** tại TP.HCM và **16 phương tiện**; lọc bán kính 5km quanh chợ Bến Thành (`onlyAvailable=true`) trả về **12 xe** sắp xếp tăng dần theo khoảng cách (`582.9 m`, `893.2 m`...). | **PASS** |
| **TC-03** | Đặt giữ xe & Chống Double-Booking | `POST /api/v1/mobility/reserve`, `POST /api/v1/mobility/vehicles/{id}/cancel-reserve` | Request đầu tiên tạo Booking `CONFIRMED` (`200 OK`); request thứ 2 đặt trùng cùng xe bị chặn ngay với **`409 Conflict`**; hủy giữ xe khôi phục trạng thái `AVAILABLE`. | **PASS** |
| **TC-04** | Vòng đời Chuyến đi & Tính cước tự động | `POST /rides/start`, `GET /rides/active`, `POST /iot/simulate-step/{id}`, `POST /rides/{id}/end`, `GET /rides/history` | Mở khóa QR (`UNLOCKED`, `IN_PROGRESS`) → mô phỏng 2 bước di chuyển GPS (`0.48 km`, pin giảm còn `96%`, tạm tính `16.210 VNĐ`) → khóa xe tại trạm trả (`LOCKED`, tổng quãng đường `3.99 km`, tổng cước `23.230 VNĐ`, trạng thái `AWAITING_PAYMENT`). | **PASS** |
| **TC-05** | Công nợ & Cổng thanh toán VNPay Sandbox | `GET /api/v1/payments/sessions/{id}/debt`, `POST /api/v1/payments` | Tra cứu đúng công nợ (`23.230 VNĐ`), khởi tạo giao dịch `Vnpay` (`PENDING`) sinh URL `sandbox.vnpayment.vn` kèm chữ ký `vnp_SecureHash` hợp lệ. | **PASS** |
| **TC-06** | IoT Telemetry, Cảnh báo Pin yếu & Điều khiển từ xa | `POST /api/v1/iot/telemetry`, `POST /api/v1/iot/vehicles/{id}/control`, `GET /api/v1/iot/telemetry/{id}` | Gửi gói tin pin `12%` tự động chuyển trạng thái xe sang **`LOW_BATTERY`**; gửi lệnh điều khiển từ xa `RECHARGE_100` sạc đầy `100%` và khôi phục **`AVAILABLE`**; lưu đầy đủ lịch sử telemetry. | **PASS** |
| **TC-07** | Luồng dữ liệu Thời gian thực (SSE) | `GET /api/v1/iot/stream` | Kết nối SSE nhận tức thời sự kiện `event:fleet-telemetry` kèm JSON tọa độ GPS, `% pin`, `lockStatus`, `fleetStatus` khi trạng thái xe thay đổi. | **PASS** |
| **TC-08** | Giao diện Web Admin & Build Mobile Android | Next.js (`localhost:3000/*`) & Android Gradle (`assembleDebug`) | Các route `/`, `/login`, `/admin/dashboard`, `/admin/fleet-map`, `/manager/fleet-map` đều phản hồi `HTTP 200`; gói cài đặt Android `app-debug.apk` biên dịch thành công. | **PASS** |


