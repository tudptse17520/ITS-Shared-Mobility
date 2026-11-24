# Phạm vi nghiệp vụ ITS-Shared-Mobility

Hệ thống cho thuê phương tiện chia sẻ tại các trạm xe công cộng. Khách thuê dùng ứng dụng Android để tìm xe, đặt giữ, quét QR mở khóa, trả xe và thanh toán. Quản trị viên dùng web để quản lý đội xe, trạm, tài khoản, bảng giá, phản ánh và xem báo cáo.

## Mô hình dữ liệu

| Thành phần | Vai trò |
| --- | --- |
| `Station`, `StationZone`, `StationDock` | Trạm xe, khu trong trạm và vị trí để xe. |
| `Vehicle`, `VehicleType` | Xe và loại xe trong đội xe chia sẻ. |
| `Booking` | Lượt đặt giữ xe, có thể được sử dụng khi bắt đầu chuyến đi. |
| `RideSession` | Chuyến đi với mã chuyến, giờ bắt đầu và kết thúc, quãng đường và tổng cước. |
| `Payment`, `PricingPolicy` | Giao dịch, tiền cọc và chính sách tính cước. |
| `VehicleTelemetryLog` | Lịch sử GPS, mức pin và trạng thái khóa từ mô phỏng IoT. |

## Ba phân hệ

- **Backend Spring Boot** cung cấp API xác thực, đội xe, trạm, đặt giữ, chuyến đi và thanh toán VNPay Sandbox. Mô phỏng IoT gửi dữ liệu đội xe qua SSE.
- **Web quản trị** có bản đồ đội xe tại `/admin/fleet-map`, quản lý trạm tại `/manager/stations`, theo dõi lượt giữ tại `/manager/bookings` và báo cáo tại `/manager/reports`.
- **Android** hỗ trợ tìm xe theo vị trí, mở khóa QR, theo dõi chuyến đi, lịch sử và thanh toán trong Sandbox.

Báo cáo đếm chuyến bắt đầu và kết thúc theo từng thời điểm. Tỷ lệ sử dụng dock là số liệu hiện tại tại các khu trong trạm. [Xem tài liệu kiểm thử](BAO_CAO_THUC_NGHIEM_ITS.md).
