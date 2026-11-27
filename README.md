# ITS-Shared-Mobility

Hệ thống quản lý chia sẻ phương tiện giao thông thông minh (**Intelligent Transportation System – Shared Mobility**), gồm 3 phân hệ:
1. **Backend API (`backend/`)**: Spring Boot 3.5 + Java 17 + Microsoft SQL Server 2022 + Spring Data JPA (`ddl-auto=update`) + JWT + VNPay Sandbox + SSE Realtime + Pessimistic Lock (`UPDLOCK, ROWLOCK`).
2. **Web Admin & Driver Portal (`frontend/`)**: Next.js 16 (App Router + `proxy.ts`), TypeScript, TanStack Query, Leaflet + VietMap, SSE telemetry realtime.
3. **Mobile Android App (`mobile-android/`)**: Kotlin + Jetpack Compose, bản đồ VietMap/OpenStreetMap, quét mã QR, mô phỏng IoT GPS, giữ phiên JWT và thanh toán VNPay Sandbox.

---

## Cấu trúc thư mục

```text
ITS-Shared-Mobility/
├── backend/                  # Spring Boot REST API + JPA/Hibernate entities
├── frontend/                 # Next.js 16 Admin & Driver web portal
├── mobile-android/           # Kotlin Jetpack Compose Android client
├── docs/                     # Báo cáo thực nghiệm & minh chứng E2E
└── .github/workflows/ci.yml  # GitHub Actions CI cho Web, Backend & Android
```

---

## Hướng dẫn cài đặt & cấu hình biến môi trường

> **Lưu ý bảo mật:** Không commit file `.env`, `.env.local`, hay `local.properties` lên Git. Sử dụng các file `.example` đi kèm làm mẫu.

### 1. Backend (`backend/`)
- Yêu cầu: **JDK 17**, **SQL Server 2022** (hoặc Azure SQL Edge / Docker SQL Server).
- Sao chép cấu hình mẫu:
  ```bash
  cp backend/.env.example backend/.env
  ```
- Các biến môi trường quan trọng:
  - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`: Chuỗi kết nối SQL Server (chạy trên DB sạch; Spring Data JPA / Hibernate tự động khởi tạo lược đồ `ddl-auto=update` kèm `open-in-view=false`).
  - `JWT_SECRET`: Khóa bí mật JWT (tối thiểu 32 ký tự).
  - `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`: Cấu hình cổng thanh toán VNPay Sandbox (không có giá trị mặc định nhạy cảm trong source).
  - `ITS_DEMO_SEED` (tùy chọn, mặc định `false`): Đặt `true` nếu muốn tự động seed dữ liệu mẫu (trạm xe, phương tiện, chính sách giá, tài khoản demo).
- Chạy Backend & bộ kiểm thử:
  ```bash
  cd backend
  ./mvnw spring-boot:run
  ./mvnw test
  ```

### 2. Web Admin & Driver Portal (`frontend/`)
- Yêu cầu: **Node.js >= 20**.
- Sao chép biến môi trường mẫu:
  ```bash
  cp frontend/.env.example frontend/.env.local
  ```
  - `NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1`
  - `NEXT_PUBLIC_VIETMAP_API_KEY`: API key VietMap (nếu để trống, bản đồ Leaflet tự fallback về OpenStreetMap).
- Cài đặt và kiểm tra:
  ```bash
  cd frontend
  npm ci
  npx tsc --noEmit
  npm run lint
  npm run build
  npm run dev
  ```

### 3. Mobile Android (`mobile-android/`)
- Yêu cầu: **Android SDK** & **JDK 17**.
- Sao chép cấu hình mẫu `mobile-android/local.properties.example` sang `mobile-android/local.properties` và điền `sdk.dir`, `API_BASE_URL` (mặc định emulator: `http://10.0.2.2:8080/api/v1/`), `VIETMAP_API_KEY`, `MAPS_API_KEY`.
- Chạy unit test & build Debug APK:
  ```bash
  cd mobile-android
  ./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon
  ```

---

## Kiểm thử tự động & CI

Workflow CI tại [.github/workflows/ci.yml](.github/workflows/ci.yml) tự động kiểm tra 3 job song song trên mỗi push/PR:
- `web`: `npm ci`, `npx tsc --noEmit`, `npm run lint`, `npm run build`
- `backend`: SQL Server 2022 container (`mcr.microsoft.com/mssql/server:2022-latest`), tạo DB sạch, chạy toàn bộ `./mvnw test` (khởi tạo lược đồ DB sạch qua JPA/Hibernate, bảo mật JWT/SSE, `SystemConfig` runtime, concurrency Pessimistic Lock, vòng đời chuyến đi).
- `android`: `./gradlew :app:testDebugUnitTest :app:assembleDebug`

Xem chi tiết kiến trúc và kết quả kiểm thử trong [docs/BAO_CAO_THUC_NGHIEM_ITS.md](docs/BAO_CAO_THUC_NGHIEM_ITS.md).
