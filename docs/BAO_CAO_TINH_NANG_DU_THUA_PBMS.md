# Báo cáo Rà soát Tính năng Dư thừa (PBMS) so với Dàn ý Đề tài ITS Shared Mobility

Tài liệu này rà soát toàn bộ mã nguồn `Project_ITS` (Backend, Web Frontend, Mobile Android) dựa trên phạm vi, mục tiêu và kiến trúc trong **`Dàn ý ITS đề tài Xây dựng ứng dụng cho thuê phương tiện giao thông.docx`**.

---

## 1. So sánh Phạm vi: Dàn ý ITS vs. Nền tảng PBMS kế thừa

| Tiêu chí | Yêu cầu theo **Dàn ý ITS (Shared Mobility)** | Nguồn kế thừa **PBMS (`Java-Project`)** |
| :--- | :--- | :--- |
| **Bài toán cốt lõi** | Cho thuê phương tiện giao thông chia sẻ (xe đạp, xe máy điện) theo mô hình Free-floating / Station-based & quản lý đội xe (Fleet Management). | Quản lý bãi đỗ xe tòa nhà (gửi xe theo tầng, slot đỗ ô tô/xe máy, kiểm soát vé vào/ra tại barie). |
| **Tác nhân (Actors)** | **2 tác nhân chính**: Khách hàng (`USER` — Mobile App Android) và Quản trị viên (`ADMIN` — Web Admin). | 4 vai trò: `ADMIN`, `MANAGER`, `STAFF` (nhân viên barie), `USER` (chủ xe đi gửi) — **đã loại bỏ `MANAGER` & `STAFF`**. |
| **3 Phân hệ** | 1. **Backend API**: Spring Boot + SQL Server + JWT + Swagger.<br>2. **Mobile App**: Kotlin Android (Bản đồ, QR mở khóa, IoT Simulator, VNPay).<br>3. **Web Admin**: Dashboard giám sát đội xe, quản lý người dùng, thống kê doanh thu. | 1. Web quản trị bãi xe tòa nhà, tầng đỗ, ô đỗ.<br>2. Giao diện nhân viên check-in / check-out cổng.<br>3. Giao diện khách tìm slot đỗ xe tòa nhà. |
| **Giới hạn IoT** | Không dùng phần cứng thật; mô phỏng tọa độ GPS, % pin, trạng thái khóa (`LOCKED`/`UNLOCKED`) qua REST API & SSE. | Check-in/check-out biển số xe tại cổng ra vào bãi đỗ. |

---

## 2. Danh sách Tính năng & Phân quyền Dư thừa từ PBMS (Đã loại bỏ hoàn toàn)

> Toàn bộ các màn hình, module Frontend, Controller Backend và 2 phân quyền ngoài kế hoạch (`MANAGER`, `STAFF`) dưới đây **đã được loại bỏ hoàn toàn khỏi hệ thống** để bám sát 100% Dàn ý đề tài ITS Shared Mobility (các Entity JPA lõi như `Building`, `Floor`, `ParkingSlot`, `Booking`, `ParkingSession` được giữ nguyên ở tầng Schema DB để tránh lỗi khóa ngoại SQL Server):

| Nhóm tính năng / Phân quyền | Đường dẫn Web (`frontend/src/app`) đã xóa | Thành phần Backend đã xử lý | Mô tả nghiệp vụ cũ (PBMS) & Lý do dư thừa | Trạng thái |
| :--- | :--- | :--- | :--- | :--- |
| **Phân quyền `MANAGER` & `STAFF`** | `/manager/dashboard`, `/manager/fleet-map`, `/staff/*` | `Role.java`, `DataSeeder.java`, `JwtAuthenticationFilter.java`, `@PreAuthorize` | Dàn ý ITS chỉ định nghĩa **2 tác nhân**: `ADMIN` (Quản trị viên) & `USER` (Khách thuê). | **Đã loại bỏ hoàn toàn** |
| **Quản lý Tầng đỗ** | `/manager/floors`, `features/floors`, `floor.service.ts`, `floor-status.ts`, `FloorResponse` | Xóa `FloorController.java`, `FloorService*`, `FloorRequestDTO.java`, `FloorResponseDTO.java` | Chia tầng hầm (`B1`, `B2`...) cho bãi giữ xe tòa nhà. | **Đã loại bỏ hoàn toàn** |
| **Quản lý Vị trí đỗ (Slot)** | `/manager/slots`, `features/slots`, `slot.service.ts`, `slot-status.ts` | Xóa `ParkingSlotController.java`, `ParkingSlotService*`, `ParkingSlot*.java`, `SlotUpdateRequest.java` | Quản lý từng ô đỗ `A01`, `A02` trong bãi đỗ xe tòa nhà. | **Đã loại bỏ hoàn toàn** |
| **Check-in / Check-out cổng & Ngoại lệ barie** | `/staff/check-in`, `/staff/check-out`, `/staff/dashboard`, `/staff/sessions`, `/staff/payments`, `/staff/feedbacks`, `features/sessions` (14 file cũ), `features/payments` (10 file UI cũ), `download-ticket.ts` | Xóa `CheckoutController.java`, `CheckoutService*`, `SessionExceptionController.java`, `ViolationController.java`, `SessionServiceImpl.java`, `Checkin*`, `CheckOut*`, `FeeCalculationResponse`, `InvoiceDTO`, `ExceptionRequestDTOs`, `LostTicketRequest`, `OverrideCheckoutRequest`, `PaymentExceptionHandler`, `LicensePlateMismatchException`, `PaymentRequiredException`, `UnpaidExitException` | Nghiệp vụ nhân viên trực barie (`STAFF`) quẹt vé vào/ra, mất vé, xe đỗ sai slot. | **Đã loại bỏ hoàn toàn** |
| **Khách tìm bãi đỗ xe trên Web** | `/browse`, `features/browse-building`, `browse-building.service.ts` | Gỡ bỏ `GET /buildings/browse`, `GET /buildings/detail/{id}`, `BuildingBrowseResponseDTO.java` | Giao diện web để tài xế ô tô tìm slot trống trong bãi đỗ. Khách thuê xe tìm xe trên Mobile App Android. | **Đã loại bỏ hoàn toàn** |
| **Đăng ký phương tiện cá nhân** | `/vehicles`, `features/vehicles`, `vehicle.service.ts`, `format-plate.ts` | Xóa `VehicleController.java`, `VehicleService*`, `VehicleRequestDTO.java`, `VehicleCreateResponseDTO.java`, `VehicleResponseDTO.java`, `PlateValidationRequest.java` | Cho phép người gửi xe khai báo biển số xe máy/ô tô cá nhân. Trong ITS, xe thuộc sở hữu đội xe công cộng. | **Đã loại bỏ hoàn toàn** |
| **Đặt slot cá nhân, Mock & Layout trùng** | `/reservations`, `/user/*`, `booking-modal.tsx`, `src/mocks/*` (4 file), `components/layouts/header.tsx`, `components/layouts/sidebar.tsx`, `dashboard-recent-activity.tsx`, 52 file `.gitkeep` & 25 thư mục rỗng | Dùng `SharedMobilityController.java` | Phiên bản web cũ, dữ liệu giả (mock) và file mẫu không còn sử dụng. | **Đã loại bỏ hoàn toàn** |

---

## 3. Ánh xạ Các Bảng CSDL Tái sử dụng (Repurposed Entities)

Để không phải đập bỏ toàn bộ Schema SQL Server có sẵn, hệ thống giữ nguyên tên bảng/Entity gốc ở tầng CSDL nhưng **tái định nghĩa ý nghĩa nghiệp vụ** sang mô hình ITS Shared Mobility:

| Entity / Bảng DB | Ý nghĩa gốc (PBMS) | Vai trò mới trong **Project_ITS (Shared Mobility)** |
| :--- | :--- | :--- |
| **`Building`** | Tòa nhà có bãi đỗ xe | **Trạm xe công cộng (Station Hub)**: Lưu 6 trạm tại TP.HCM (UTH Cơ sở 1, Ga Metro Văn Thánh, Vinhomes Tân Cảng, Phố đi bộ Nguyễn Huệ, NVH Thanh Niên, UTH Cơ sở 3) kèm tọa độ `latitude`, `longitude`, `totalDocks`. |
| **`Vehicle`** | Xe cá nhân của khách | **Phương tiện đội xe chia sẻ (Fleet Vehicle)**: Lưu 16 xe (`ITS-BK-*`, `ITS-EB-*`, `ITS-ES-*`, `ITS-KS-*`) kèm `vehicleCode` (mã QR), `batteryLevel`, `lockStatus`, `fleetStatus`, GPS realtime. |
| **`VehicleType`** | Phân loại xe gửi bãi | **Loại xe cho thuê**: Xe đạp cơ (`Bicycle`), Xe đạp trợ lực điện (`E-Bike`), Xe máy điện (`E-Scooter`), Xe Kick-Scooter điện. |
| **`PricingPolicy`** | Bảng giá gửi xe theo giờ | **Bảng giá thuê xe**: Lưu phí mở khóa (`unlockFee`) và giá thuê theo giờ (`hourlyRate`). |
| **`Booking`** | Đặt trước chỗ đỗ xe | **Đặt giữ xe 15 phút**: Áp dụng Pessimistic Write Lock (`findByIdForUpdate`) chống hai người cùng đặt 1 xe đồng thời (`409 Conflict`). |
| **`ParkingSession`** | Lượt gửi xe trong hầm | **Phiên chuyến đi (Ride Session)**: Lưu thời gian thuê, tọa độ bắt đầu/kết thúc, quãng đường `distanceKm`, pin tiêu hao và tổng cước. |
| **`Payment`** | Thanh toán phí gửi xe | **Thanh toán cước chuyến đi**: Tích hợp cổng **VNPay Sandbox** (`PaymentController`). |

---

## 4. Các Module Cốt lõi Phù hợp 100% với Dàn ý Tiểu luận ITS

Đây là các phân hệ chính thức được giữ lại, hoàn thiện và dùng để quay video demo / viết báo cáo:

1. **Backend API (`backend/`)**:
   - `AuthController`: Đăng ký, đăng nhập cấp JWT Token, lấy thông tin user (`/auth/login`, `/auth/register`, `/auth/me`).
   - `SharedMobilityController`: Danh sách trạm (`/mobility/stations`), tìm xe gần nhất theo công thức Haversine (`/mobility/vehicles/nearby`), đặt giữ xe có Pessimistic Lock (`/mobility/reserve`), hủy giữ (`/mobility/vehicles/{id}/cancel-reserve`), mở khóa QR bắt đầu chuyến (`/mobility/rides/start`), kết thúc & tính cước (`/mobility/rides/{id}/end`), lịch sử chuyến đi (`/mobility/rides/history`).
   - `IotSimulationController`: Điều khiển khóa thông minh, sạc pin, mô phỏng bước di chuyển GPS (`/iot/simulate-step/{sessionId}`), luồng **SSE Realtime (`/iot/stream`)** và lịch sử telemetry.
   - `PaymentController`: Tạo link thanh toán **VNPay Sandbox** và xử lý IPN/Return callback.
   - `ConcurrencyReservationTest`: Kiểm thử thực nghiệm 50 request đồng thời (Chương 4/5 tiểu luận).

2. **Mobile App Kotlin Android (`mobile-android/`)**:
   - `AuthScreen`: Đăng ký / Đăng nhập JWT.
   - `HomeMapScreen`: Bản đồ **VietMap + OpenStreetMap** hiển thị 6 trạm và 16 xe, bộ lọc loại xe/trạng thái, đặt giữ / hủy giữ, lịch sử chuyến đi.
   - `QrScannerDialog`: Hiển thị tem mã QR thực tế (`its://unlock?code=...`), giải mã QR bằng ZXing, nhập tay hoặc chọn nhanh mã xe để mở khóa.
   - `ActiveRideScreen`: Bản đồ GPS trực tiếp, bộ đếm thời gian/quãng đường/pin/cước tạm tính, nút mô phỏng di chuyển GPS (`+300m / IoT Sim`), nút khóa xe kết thúc chuyến.
   - `PaymentVnpayScreen`: Hóa đơn tổng cước và WebView thanh toán **VNPay Sandbox**.

3. **Web Admin Dashboard (`frontend/`)**:
   - **Bản đồ Đội xe & Giả lập IoT** (`/admin/fleet-map`): Giám sát toàn bộ xe realtime qua SSE, hiển thị **Tem Mã QR** từng xe, điều khiển IoT, Auto-Simulation lộ trình và nút thực nghiệm Concurrency 20 request.
   - **Quản lý Trạm xe, Loại xe, Bảng giá, Đặt giữ & Chuyến đi, Người dùng, Sự cố và Thống kê Doanh thu** (`/admin/dashboard`, `/admin/users`, `/manager/buildings`, `/manager/vehicle-types`, `/manager/pricing`, `/manager/bookings`, `/manager/feedbacks`, `/manager/reports`).

