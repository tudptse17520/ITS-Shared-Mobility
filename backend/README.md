# Backend Structure

```text
vn\edu\ut\its
├── config/       # Cấu hình CORS, Async, Swagger và Security (JWT)
├── constant/     # Chứa các Enum (SlotStatus, VehicleType,...)
├── controller/   # Nơi tiếp nhận request từ React/Next.js
├── dto/          # Dữ liệu luân chuyển giữa FE và BE
│   ├── request/
│   └── response/
├── entity/       # Map trực tiếp với các bảng trong SQL Server
├── exception/    # Xử lý lỗi tập trung (GlobalExceptionHandler)
├── repository/   # Giao tiếp với SQL Server thông qua JPA
├── service/      # Nơi xử lý logic (Tính phí chuyến đi, ...)
│   └── impl/     # Hiện thực hóa các Interface của Service
└── util/         # Các hàm tiện ích (mã hóa mật khẩu, xử lý chuỗi, định dạng ngày tháng)
```

## Prerequisites
- Java 17
- Maven
- SQL Server

## Setup & Run
1. Navigate to the backend directory:
   ```bash
   cd backend/
   ```
2. Set up environment variables:
   Copy `.env.example` to `.env` and update the database credentials and JWT secret. Use a disposable SQL Server database for integration tests because the demo seeder clears tables at startup.
3. Install dependencies and compile the project:
   ```bash
   .\mvnw.cmd clean compile
   ```
4. Run the application:
   ```bash
   .\mvnw.cmd spring-boot:run
   ```
The backend server will start on `http://localhost:8080`.
