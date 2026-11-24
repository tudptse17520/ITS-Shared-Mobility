# Backend Structure

```text
vn\edu\ut\its
├── config/       # Cấu hình CORS, Async, Swagger và Security (JWT)
├── constant/     # Chứa các Enum (StationDockStatus, RideSessionStatus, VehicleTypeStatus,...)
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
   Copy `.env.example` to `.env` and update the database credentials and JWT secret. Keep `APP_SECURITY_REQUIRE_AUTH=true` in normal runs. Set `ITS_DEMO_SEED=true` (or `APP_DEMO_SEED_ENABLED=true`) only for a disposable demo database; the seeder resets demo data when enabled. For an existing database, see the migration instructions below.
3. Install dependencies and compile the project:
   ```bash
   .\mvnw.cmd clean compile
   ```
4. Run the application:
   ```bash
   .\mvnw.cmd spring-boot:run
   ```
The backend server will start on `http://localhost:8080`.

## Upgrade an existing database

Back up the SQL Server database and test the upgrade on a copy. Stop the backend and run [migrate-its-schema.sql](scripts/migrate-its-schema.sql) against the intended database before starting the new version. Leave `ITS_DEMO_SEED` unset or false to preserve existing records. A fresh database can start directly with the current schema.

Historical payment adjustments remain in transaction history and are excluded from ride revenue. Review the upgraded records and start the backend against the copy before rolling out the upgrade.
