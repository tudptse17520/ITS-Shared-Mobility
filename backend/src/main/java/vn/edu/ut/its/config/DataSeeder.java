package vn.edu.ut.its.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ut.its.constant.*;
import vn.edu.ut.its.entity.*;
import vn.edu.ut.its.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeder khởi tạo dữ liệu mẫu cho hệ thống ITS-Shared-Mobility.
 * Bao gồm 6 trạm xe tại TP.HCM, 4 loại phương tiện chia sẻ, 16 phương tiện gắn GPS giả lập,
 * các chuyến thuê xe, thanh toán VNPay và nhật ký Telemetry.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final PricingPolicyRepository pricingPolicyRepository;
    private final StationRepository stationRepository;
    private final StationZoneRepository stationZoneRepository;
    private final StationDockRepository stationDockRepository;
    private final VehicleRepository vehicleRepository;
    private final RideSessionRepository sessionRepository;
    private final PaymentRepository paymentRepository;
    private final FeedbackRepository feedbackRepository;
    private final BookingRepository bookingRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final VehicleTelemetryLogRepository telemetryLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== BẮT ĐẦU CHẠY DATABASE SEEDER (ITS-Shared-Mobility) ===");

        // Delete strictly from leaf/child tables to root/parent tables to satisfy SQL Server FKs.
        telemetryLogRepository.deleteAllInBatch();
        feedbackRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        sessionRepository.deleteAllInBatch();
        bookingRepository.deleteAllInBatch();
        vehicleRepository.deleteAllInBatch();
        stationDockRepository.deleteAllInBatch();
        stationZoneRepository.deleteAllInBatch();
        stationRepository.deleteAllInBatch();
        pricingPolicyRepository.hardDeleteAllInBatch();
        vehicleTypeRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        systemConfigRepository.deleteAllInBatch();

        List<User> users = seedUsers();
        List<VehicleType> vehicleTypes = seedVehicleTypesAndPricing();
        List<Station> stations = seedStationsAndDocks(vehicleTypes);
        List<Vehicle> fleet = seedFleetVehicles(users, vehicleTypes, stations);
        seedOperations(users, fleet, stations);
        seedSystemConfigs();

        log.info("=== KẾT THÚC SEEDER ITS-Shared-Mobility THÀNH CÔNG ({} trạm, {} xe) ===", stations.size(), fleet.size());
    }

    private void seedSystemConfigs() {
        List<SystemConfig> configs = List.of(
                SystemConfig.builder().configKey("HOLD_DURATION_MINUTES").configValue("15").description("Thời gian giữ xe tối đa khi đặt trước (phút)").build(),
                SystemConfig.builder().configKey("LOW_BATTERY_THRESHOLD").configValue("15").description("Ngưỡng cảnh báo pin yếu (%)").build(),
                SystemConfig.builder().configKey("DISTANCE_RATE_PER_KM").configValue("2000").description("Phụ phí theo quãng đường (VNĐ/km)").build(),
                SystemConfig.builder().configKey("SEARCH_RADIUS_KM").configValue("5").description("Bán kính tìm xe mặc định (km)").build()
        );
        systemConfigRepository.saveAll(configs);
    }

    private List<User> seedUsers() {
        String defaultPassword = passwordEncoder.encode("password");
        List<User> users = List.of(
                User.builder().username("admin").password(defaultPassword).fullName("Nguyễn Trần Quản Trị").phoneNumber("0901234567").role(Role.ADMIN).status(UserStatus.ACTIVE).build(),
                User.builder().username("user").password(defaultPassword).fullName("Trần Khách Thuê").phoneNumber("0934567890").role(Role.USER).status(UserStatus.ACTIVE).build(),
                User.builder().username("thanh.tu").password(defaultPassword).fullName("Đoàn Phạm Thanh Tú").phoneNumber("0987654321").role(Role.USER).status(UserStatus.ACTIVE).build(),
                User.builder().username("hoang.nam").password(defaultPassword).fullName("Trần Hoàng Nam").phoneNumber("0888123456").role(Role.USER).status(UserStatus.ACTIVE).build(),
                User.builder().username("minh.nguyet").password(defaultPassword).fullName("Lê Thị Minh Nguyệt").phoneNumber("0777987654").role(Role.USER).status(UserStatus.ACTIVE).build()
        );
        return userRepository.saveAll(users);
    }

    private List<VehicleType> seedVehicleTypesAndPricing() {
        VehicleType smartBike = VehicleType.builder()
                .typeName("Xe Đạp Công Cộng (Smart Bike)")
                .description("Xe đạp cơ khóa thông minh QR, phù hợp chặng ngắn nội đô")
                .status(VehicleTypeStatus.ACTIVE).build();
        VehicleType eBike = VehicleType.builder()
                .typeName("Xe Đạp Trợ Lực Điện (E-Bike)")
                .description("Xe đạp điện trợ lực pin Lithium, tầm hoạt động 60km")
                .status(VehicleTypeStatus.ACTIVE).build();
        VehicleType eScooter = VehicleType.builder()
                .typeName("Xe Máy Điện Đô Thị (E-Scooter)")
                .description("Xe máy điện VinFast Evo200 / Dat Bike cho di chuyển linh hoạt")
                .status(VehicleTypeStatus.ACTIVE).build();
        VehicleType eScooterPro = VehicleType.builder()
                .typeName("Xe Máy Điện Cao Cấp (Klara S)")
                .description("Xe máy điện cao cấp pin LFP hiệu suất cao, cốp rộng")
                .status(VehicleTypeStatus.ACTIVE).build();

        List<VehicleType> savedTypes = vehicleTypeRepository.saveAll(List.of(smartBike, eBike, eScooter, eScooterPro));

        // basePrice = Phí mở khóa (VNĐ), extraFeePerHour = Đơn giá thuê theo giờ (VNĐ/giờ)
        List<PricingPolicy> policies = List.of(
                PricingPolicy.builder().vehicleType(savedTypes.get(0)).basePrice(BigDecimal.valueOf(5000)).extraFeePerHour(BigDecimal.valueOf(5000)).effectiveDate(LocalDateTime.now().minusMonths(3)).status(PricingPolicyStatus.ACTIVE).build(),
                PricingPolicy.builder().vehicleType(savedTypes.get(1)).basePrice(BigDecimal.valueOf(10000)).extraFeePerHour(BigDecimal.valueOf(10000)).effectiveDate(LocalDateTime.now().minusMonths(3)).status(PricingPolicyStatus.ACTIVE).build(),
                PricingPolicy.builder().vehicleType(savedTypes.get(2)).basePrice(BigDecimal.valueOf(15000)).extraFeePerHour(BigDecimal.valueOf(15000)).effectiveDate(LocalDateTime.now().minusMonths(3)).status(PricingPolicyStatus.ACTIVE).build(),
                PricingPolicy.builder().vehicleType(savedTypes.get(3)).basePrice(BigDecimal.valueOf(20000)).extraFeePerHour(BigDecimal.valueOf(20000)).effectiveDate(LocalDateTime.now().minusMonths(3)).status(PricingPolicyStatus.ACTIVE).build()
        );
        pricingPolicyRepository.saveAll(policies);

        return savedTypes;
    }

    private List<Station> seedStationsAndDocks(List<VehicleType> types) {
        List<Station> stations = stationRepository.saveAll(List.of(
                Station.builder()
                        .stationName("Trạm ĐH GTVT TP.HCM (UTH Cơ sở 1)")
                        .address("Số 2 Võ Oanh, Phường 25, Bình Thạnh, TP.HCM")
                        .numberOfZones(1).latitude(10.8045).longitude(106.7166).totalDocks(25)
                        .status(StationStatus.ACTIVE).build(),
                Station.builder()
                        .stationName("Trạm Ga Metro Văn Thánh")
                        .address("Điện Biên Phủ, Phường 25, Bình Thạnh, TP.HCM")
                        .numberOfZones(1).latitude(10.7992).longitude(106.7155).totalDocks(20)
                        .status(StationStatus.ACTIVE).build(),
                Station.builder()
                        .stationName("Trạm Công Viên Vinhomes Tân Cảng")
                        .address("208 Nguyễn Hữu Cảnh, Phường 22, Bình Thạnh, TP.HCM")
                        .numberOfZones(1).latitude(10.7950).longitude(106.7218).totalDocks(30)
                        .status(StationStatus.ACTIVE).build(),
                Station.builder()
                        .stationName("Trạm Phố Đi Bộ Nguyễn Huệ - Bến Bạch Đằng")
                        .address("Số 2 Tôn Đức Thắng, Bến Nghé, Quận 1, TP.HCM")
                        .numberOfZones(1).latitude(10.7738).longitude(106.7064).totalDocks(30)
                        .status(StationStatus.ACTIVE).build(),
                Station.builder()
                        .stationName("Trạm Nhà Văn Hóa Thanh Niên")
                        .address("Số 4 Phạm Ngọc Thạch, Bến Nghé, Quận 1, TP.HCM")
                        .numberOfZones(1).latitude(10.7825).longitude(106.6980).totalDocks(20)
                        .status(StationStatus.ACTIVE).build(),
                Station.builder()
                        .stationName("Trạm ĐH GTVT TP.HCM (UTH Cơ sở 3)")
                        .address("70 Tô Ký, Tân Chánh Hiệp, Quận 12, TP.HCM")
                        .numberOfZones(1).latitude(10.8654).longitude(106.6182).totalDocks(25)
                        .status(StationStatus.ACTIVE).build()
        ));

        // Tạo 1 tầng & dock mẫu cho mỗi trạm để tương thích ngược với các màn hình ITS cũ
        List<StationZone> stationZones = new ArrayList<>();
        for (Station st : stations) {
            stationZones.add(StationZone.builder()
                    .station(st)
                    .vehicleType(types.get(2))
                    .zoneName("Khu Dock Đỗ Xe - " + st.getStationName())
                    .zoneLevel(1)
                    .capacity(st.getTotalDocks() != null ? st.getTotalDocks() : 20)
                    .status(StationZoneStatus.ACTIVE)
                    .build());
        }
        List<StationZone> savedZones = stationZoneRepository.saveAll(stationZones);

        List<StationDock> docks = new ArrayList<>();
        for (int sIdx = 0; sIdx < savedZones.size(); sIdx++) {
            StationZone fl = savedZones.get(sIdx);
            for (int i = 1; i <= 6; i++) {
                docks.add(StationDock.builder()
                        .stationZone(fl)
                        .dockName("DOCK-" + (sIdx + 1) + "-" + String.format("%02d", i))
                        .status(StationDockStatus.AVAILABLE)
                        .build());
            }
        }
        stationDockRepository.saveAll(docks);
        return stations;
    }

    private List<Vehicle> seedFleetVehicles(List<User> users, List<VehicleType> types, List<Station> stations) {
        User fleetOwner = users.get(0); // Admin / Fleet System

        List<Vehicle> fleet = List.of(
                // Trạm 1: UTH Cơ sở 1 (Bình Thạnh)
                buildFleet(fleetOwner, types.get(1), stations.get(0), "ITS-EB-001", "59MĐ1-001.01", "Yadea i5S E-Bike", "Xanh Ngọc", 10.8047, 106.7168, 94, "LOCKED", "AVAILABLE"),
                buildFleet(fleetOwner, types.get(2), stations.get(0), "ITS-ES-002", "59MĐ1-002.02", "VinFast Evo200", "Trắng Sứ", 10.8043, 106.7164, 88, "LOCKED", "AVAILABLE"),
                buildFleet(fleetOwner, types.get(0), stations.get(0), "ITS-BK-003", "ITS-BIKE-003", "TNGo Smart Bike", "Cam Đô Thị", 10.8049, 106.7162, 100, "LOCKED", "AVAILABLE"),
                buildFleet(fleetOwner, types.get(3), stations.get(0), "ITS-KS-004", "59MĐ1-004.04", "VinFast Klara S2", "Xanh Lục", 10.8041, 106.7170, 76, "LOCKED", "AVAILABLE"),

                // Trạm 2: Ga Metro Văn Thánh
                buildFleet(fleetOwner, types.get(1), stations.get(1), "ITS-EB-005", "59MĐ1-005.05", "Engwe C20 Pro", "Xám Titan", 10.7994, 106.7157, 82, "LOCKED", "AVAILABLE"),
                buildFleet(fleetOwner, types.get(2), stations.get(1), "ITS-ES-006", "59MĐ1-006.06", "VinFast Feliz S", "Đen Nhám", 10.7990, 106.7153, 91, "LOCKED", "AVAILABLE"),
                buildFleet(fleetOwner, types.get(0), stations.get(1), "ITS-BK-007", "ITS-BIKE-007", "TNGo Smart Bike", "Cam Đô Thị", 10.7996, 106.7151, 100, "LOCKED", "AVAILABLE"),

                // Trạm 3: Vinhomes Tân Cảng
                buildFleet(fleetOwner, types.get(2), stations.get(2), "ITS-ES-008", "59MĐ1-008.08", "Dat Bike Quantum", "Xanh Dương", 10.7952, 106.7220, 68, "LOCKED", "AVAILABLE"),
                buildFleet(fleetOwner, types.get(3), stations.get(2), "ITS-KS-009", "59MĐ1-009.09", "VinFast Klara S2", "Trắng Ngọc", 10.7948, 106.7215, 13, "LOCKED", "LOW_BATTERY"),
                buildFleet(fleetOwner, types.get(1), stations.get(2), "ITS-EB-010", "59MĐ1-010.10", "ADO A20 Air", "Trắng Bạc", 10.7955, 106.7217, 79, "UNLOCKED", "IN_USE"),

                // Trạm 4: Phố đi bộ Nguyễn Huệ - Bến Bạch Đằng
                buildFleet(fleetOwner, types.get(0), stations.get(3), "ITS-BK-011", "ITS-BIKE-011", "TNGo Smart Bike", "Cam Đô Thị", 10.7740, 106.7066, 100, "LOCKED", "AVAILABLE"),
                buildFleet(fleetOwner, types.get(1), stations.get(3), "ITS-EB-012", "59MĐ1-012.12", "Yadea i5S E-Bike", "Xanh Ngọc", 10.7736, 106.7061, 85, "LOCKED", "AVAILABLE"),
                buildFleet(fleetOwner, types.get(2), stations.get(3), "ITS-ES-013", "59MĐ1-013.13", "VinFast Evo200", "Đỏ Đô", 10.7742, 106.7060, 45, "LOCKED", "MAINTENANCE"),

                // Trạm 5: Nhà Văn Hóa Thanh Niên (Quận 1)
                buildFleet(fleetOwner, types.get(2), stations.get(4), "ITS-ES-014", "59MĐ1-014.14", "VinFast Feliz S", "Bạc Ánh Kim", 10.7827, 106.6983, 95, "LOCKED", "AVAILABLE"),
                buildFleet(fleetOwner, types.get(0), stations.get(4), "ITS-BK-015", "ITS-BIKE-015", "TNGo Smart Bike", "Cam Đô Thị", 10.7823, 106.6978, 100, "LOCKED", "AVAILABLE"),

                // Trạm 6: UTH Cơ sở 3 (Quận 12)
                buildFleet(fleetOwner, types.get(2), stations.get(5), "ITS-ES-016", "59MĐ1-016.16", "VinFast Evo200", "Xanh Rêu", 10.8655, 106.6184, 90, "LOCKED", "AVAILABLE")
        );

        return vehicleRepository.saveAll(fleet);
    }

    private Vehicle buildFleet(
            User owner,
            VehicleType type,
            Station station,
            String code,
            String plate,
            String brand,
            String color,
            double lat,
            double lng,
            int battery,
            String lockStatus,
            String fleetStatus
    ) {
        return Vehicle.builder()
                .user(owner)
                .vehicleType(type)
                .currentStationId(station.getId())
                .vehicleCode(code)
                .plate(plate)
                .brand(brand)
                .color(color)
                .latitude(lat)
                .longitude(lng)
                .batteryLevel(battery)
                .lockStatus(lockStatus)
                .fleetStatus(fleetStatus)
                .build();
    }

    private void seedOperations(List<User> users, List<Vehicle> fleet, List<Station> stations) {
        User khach1 = users.get(1); // user
        User thanhTu = users.get(2); // thanh.tu
        User hoangNam = users.get(3); // hoang.nam

        Vehicle inUseBike = fleet.get(9);   // ITS-EB-010 (IN_USE)
        Vehicle completedBike = fleet.get(1); // ITS-ES-002
        Vehicle completedBike2 = fleet.get(5); // ITS-ES-006

        // 1. Chuyến đi đang diễn ra (IN_PROGRESS) để minh họa trên Live Fleet Map
        RideSession activeRide = RideSession.builder()
                .ticketCode("RIDE-ITS-EB-010-101")
                .plate(inUseBike.getPlate())
                .user(hoangNam)
                .vehicle(inUseBike)
                .timeIn(LocalDateTime.now().minusMinutes(18))
                .startLatitude(stations.get(2).getLatitude())
                .startLongitude(stations.get(2).getLongitude())
                .distanceKm(1.45)
                .startBatteryLevel(83)
                .status(RideSessionStatus.IN_PROGRESS)
                .build();
        activeRide = sessionRepository.save(activeRide);

        telemetryLogRepository.save(VehicleTelemetryLog.builder()
                .vehicle(inUseBike)
                .rideSession(activeRide)
                .latitude(inUseBike.getLatitude())
                .longitude(inUseBike.getLongitude())
                .batteryLevel(inUseBike.getBatteryLevel())
                .speedKmh(22.4)
                .lockStatus("UNLOCKED")
                .recordedAt(LocalDateTime.now().minusMinutes(2))
                .build());

        // 2. Các chuyến đi đã hoàn tất và thanh toán qua VNPay
        RideSession completed1 = RideSession.builder()
                .ticketCode("RIDE-ITS-ES-002-088")
                .plate(completedBike.getPlate())
                .user(thanhTu)
                .vehicle(completedBike)
                .timeIn(LocalDateTime.now().minusHours(4))
                .timeOut(LocalDateTime.now().minusHours(3).minusMinutes(15))
                .startLatitude(stations.get(3).getLatitude())
                .startLongitude(stations.get(3).getLongitude())
                .endLatitude(stations.get(0).getLatitude())
                .endLongitude(stations.get(0).getLongitude())
                .distanceKm(4.2)
                .startBatteryLevel(96)
                .endBatteryLevel(88)
                .totalFee(BigDecimal.valueOf(35000))
                .status(RideSessionStatus.COMPLETED)
                .build();
        completed1 = sessionRepository.save(completed1);

        paymentRepository.save(Payment.builder()
                .rideSession(completed1)
                .amount(BigDecimal.valueOf(35000))
                .paymentMethod(PaymentMethod.Vnpay)
                .paymentTime(LocalDateTime.now().minusHours(3).minusMinutes(14))
                .status(PaymentStatus.SUCCESS)
                .feeType(FeeType.RIDE_FEE)
                .build());

        RideSession completed2 = RideSession.builder()
                .ticketCode("RIDE-ITS-ES-006-089")
                .plate(completedBike2.getPlate())
                .user(khach1)
                .vehicle(completedBike2)
                .timeIn(LocalDateTime.now().minusHours(2))
                .timeOut(LocalDateTime.now().minusHours(1).minusMinutes(20))
                .startLatitude(stations.get(0).getLatitude())
                .startLongitude(stations.get(0).getLongitude())
                .endLatitude(stations.get(1).getLatitude())
                .endLongitude(stations.get(1).getLongitude())
                .distanceKm(2.1)
                .startBatteryLevel(95)
                .endBatteryLevel(91)
                .totalFee(BigDecimal.valueOf(29000))
                .status(RideSessionStatus.COMPLETED)
                .build();
        completed2 = sessionRepository.save(completed2);

        paymentRepository.save(Payment.builder()
                .rideSession(completed2)
                .amount(BigDecimal.valueOf(29000))
                .paymentMethod(PaymentMethod.Vnpay)
                .paymentTime(LocalDateTime.now().minusHours(1).minusMinutes(19))
                .status(PaymentStatus.SUCCESS)
                .feeType(FeeType.RIDE_FEE)
                .build());

        // 3. Phản hồi sự cố phương tiện từ khách hàng
        feedbackRepository.save(Feedback.builder()
                .rideSession(completed1)
                .issueType(IssueType.WRONG_PLACE)
                .description("Xe ITS-ES-002 trả xe hơi lệch vạch trạm UTH Cơ sở 1, đã được kỹ thuật viên điều chỉnh.")
                .status(FeedbackStatus.RESOLVED)
                .build());

        // 4. Dữ liệu đặt chỗ trước mẫu để kiểm tra màn hình Quản lý Đặt chỗ
        List<StationDock> availableDocks = stationDockRepository.findAll().stream()
                .filter(s -> s.getStatus() == StationDockStatus.AVAILABLE)
                .limit(2)
                .toList();
        if (availableDocks.size() >= 2) {
            StationDock dock1 = availableDocks.get(0);
            dock1.setStatus(StationDockStatus.RESERVED);
            StationDock dock2 = availableDocks.get(1);
            dock2.setStatus(StationDockStatus.RESERVED);
            stationDockRepository.saveAll(List.of(dock1, dock2));

            bookingRepository.saveAll(List.of(
                    Booking.builder()
                            .user(khach1)
                            .vehicle(fleet.get(0))
                            .stationDock(dock1)
                            .expectedTimeIn(LocalDateTime.now().plusHours(1))
                            .expectedTimeOut(LocalDateTime.now().plusHours(3))
                            .status(BookingStatus.CONFIRMED)
                            .build(),
                    Booking.builder()
                            .user(thanhTu)
                            .vehicle(fleet.get(4))
                            .stationDock(dock2)
                            .expectedTimeIn(LocalDateTime.now().plusHours(2))
                            .expectedTimeOut(LocalDateTime.now().plusHours(5))
                            .status(BookingStatus.PENDING)
                            .build()
            ));
        }
    }
}
