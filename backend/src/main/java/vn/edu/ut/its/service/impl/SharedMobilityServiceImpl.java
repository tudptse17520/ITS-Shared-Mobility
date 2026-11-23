package vn.edu.ut.its.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ut.its.constant.BookingStatus;
import vn.edu.ut.its.constant.StationStatus;
import vn.edu.ut.its.constant.RideSessionStatus;
import vn.edu.ut.its.constant.PaymentStatus;
import vn.edu.ut.its.dto.mobility.*;
import vn.edu.ut.its.dto.response.BookingResponseDTO;
import vn.edu.ut.its.entity.*;
import vn.edu.ut.its.exception.BusinessRuleViolationException;
import vn.edu.ut.its.exception.ConflictException;
import vn.edu.ut.its.exception.ResourceNotFoundException;
import vn.edu.ut.its.repository.*;
import vn.edu.ut.its.service.SharedMobilityService;
import vn.edu.ut.its.service.SseEmitterManager;
import vn.edu.ut.its.util.GeoUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SharedMobilityServiceImpl implements SharedMobilityService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final BigDecimal DEFAULT_UNLOCK_FEE = BigDecimal.valueOf(10000);
    private static final BigDecimal DEFAULT_HOURLY_RATE = BigDecimal.valueOf(10000);

    private final StationRepository stationRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final RideSessionRepository rideSessionRepository;
    private final PricingPolicyRepository pricingPolicyRepository;
    private final VehicleTelemetryLogRepository telemetryLogRepository;
    private final PaymentCustomRepository paymentCustomRepository;
    private final SseEmitterManager sseEmitterManager;

    @Override
    @Transactional(readOnly = true)
    public List<StationResponseDTO> getStations() {
        List<Station> stations = stationRepository.findAll();
        List<Vehicle> fleet = vehicleRepository.findByVehicleCodeIsNotNull();

        Map<Long, Long> availableCountByStation = fleet.stream()
                .filter(v -> v.getCurrentStationId() != null && "AVAILABLE".equalsIgnoreCase(v.getFleetStatus()))
                .collect(Collectors.groupingBy(Vehicle::getCurrentStationId, Collectors.counting()));

        return stations.stream()
                .filter(b -> b.getLatitude() != null && b.getLongitude() != null)
                .map(b -> StationResponseDTO.builder()
                        .id(b.getId())
                        .stationName(b.getStationName())
                        .address(b.getAddress())
                        .latitude(b.getLatitude())
                        .longitude(b.getLongitude())
                        .totalDocks(b.getTotalDocks() != null ? b.getTotalDocks() : 20)
                        .availableVehicles(availableCountByStation.getOrDefault(b.getId(), 0L).intValue())
                        .status(b.getStatus() != null ? b.getStatus().name() : StationStatus.ACTIVE.name())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FleetVehicleResponseDTO> getAllFleetVehicles() {
        Map<Long, String> stationNames = stationRepository.findAll().stream()
                .collect(Collectors.toMap(Station::getId, Station::getStationName, (a, b) -> a));

        return vehicleRepository.findByVehicleCodeIsNotNull().stream()
                .map(v -> mapToFleetVehicleDTO(v, null, stationNames))
                .sorted(Comparator.comparing(FleetVehicleResponseDTO::getId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FleetVehicleResponseDTO> getNearbyVehicles(Double lat, Double lng, Double radiusKm, Long vehicleTypeId, Boolean onlyAvailable) {
        double userLat = lat != null ? lat : 10.8045;
        double userLng = lng != null ? lng : 106.7166;
        double maxRadiusKm = radiusKm != null ? radiusKm : 15.0;
        boolean filterAvailable = Boolean.TRUE.equals(onlyAvailable);

        Map<Long, String> stationNames = stationRepository.findAll().stream()
                .collect(Collectors.toMap(Station::getId, Station::getStationName, (a, b) -> a));

        return vehicleRepository.findByVehicleCodeIsNotNull().stream()
                .filter(v -> v.getLatitude() != null && v.getLongitude() != null)
                .filter(v -> vehicleTypeId == null || (v.getVehicleType() != null && vehicleTypeId.equals(v.getVehicleType().getId())))
                .filter(v -> !filterAvailable || "AVAILABLE".equalsIgnoreCase(v.getFleetStatus()))
                .map(v -> {
                    double meters = GeoUtils.haversineMeters(userLat, userLng, v.getLatitude(), v.getLongitude());
                    return mapToFleetVehicleDTO(v, meters, stationNames);
                })
                .filter(dto -> dto.getDistanceMeters() <= maxRadiusKm * 1000.0)
                .sorted(Comparator.comparing(FleetVehicleResponseDTO::getDistanceMeters))
                .collect(Collectors.toList());
    }

    /**
     * Đặt giữ xe với Pessimistic Write Lock để xử lý cạnh tranh đồng thời (Concurrency Control).
     */
    @Override
    public BookingResponseDTO reserveVehicle(ReserveVehicleRequestDTO request) {
        User user = resolveUser(request.getUserId());

        // Khóa dòng dữ liệu xe (SELECT ... WITH (UPDLOCK, ROWLOCK) trong SQL Server)
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phương tiện với ID: " + request.getVehicleId()));

        if (!"AVAILABLE".equalsIgnoreCase(vehicle.getFleetStatus())) {
            throw new ConflictException("Phương tiện " + vehicle.getVehicleCode()
                    + " hiện đang ở trạng thái " + vehicle.getFleetStatus() + ", không thể đặt giữ.");
        }

        if (vehicle.getBatteryLevel() != null && vehicle.getBatteryLevel() < 15) {
            throw new BusinessRuleViolationException("Phương tiện " + vehicle.getVehicleCode()
                    + " đang yếu pin (" + vehicle.getBatteryLevel() + "%), vui lòng chọn xe khác.");
        }

        int holdMinutes = (request.getHoldMinutes() != null && request.getHoldMinutes() > 0)
                ? request.getHoldMinutes()
                : 15;
        LocalDateTime now = LocalDateTime.now();

        vehicle.setFleetStatus("RESERVED");
        vehicleRepository.save(vehicle);

        Booking booking = Booking.builder()
                .user(user)
                .vehicle(vehicle)
                .createdAt(now)
                .expiredAt(now.plusMinutes(holdMinutes))
                .expectedTimeIn(now)
                .expectedTimeOut(now.plusHours(2))
                .status(BookingStatus.CONFIRMED)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        broadcastFleetSnapshot(vehicle, null);

        return BookingResponseDTO.builder()
                .id(savedBooking.getId())
                .status(savedBooking.getStatus())
                .message("Đặt giữ xe " + vehicle.getVehicleCode() + " thành công trong " + holdMinutes + " phút.")
                .build();
    }

    @Override
    public void cancelReservation(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phương tiện với ID: " + vehicleId));

        if (!"RESERVED".equalsIgnoreCase(vehicle.getFleetStatus())) {
            throw new BusinessRuleViolationException("Phương tiện không ở trạng thái đặt giữ (RESERVED).");
        }

        bookingRepository.findFirstByVehicleIdAndStatusOrderByIdDesc(vehicleId, BookingStatus.CONFIRMED)
                .ifPresent(booking -> {
                    booking.setStatus(BookingStatus.CANCELLED);
                    bookingRepository.save(booking);
                });

        vehicle.setFleetStatus("AVAILABLE");
        vehicleRepository.save(vehicle);
        broadcastFleetSnapshot(vehicle, null);
    }

    @Override
    public RideSessionResponseDTO startRide(StartRideRequestDTO request) {
        User user = resolveUser(request.getUserId());

        Vehicle vehicle;
        if (request.getVehicleId() != null) {
            vehicle = vehicleRepository.findByIdForUpdate(request.getVehicleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phương tiện với ID: " + request.getVehicleId()));
        } else if (request.getVehicleCode() != null && !request.getVehicleCode().isBlank()) {
            vehicle = vehicleRepository.findByVehicleCodeForUpdate(request.getVehicleCode().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phương tiện với mã QR: " + request.getVehicleCode()));
        } else {
            throw new BusinessRuleViolationException("Vui lòng cung cấp vehicleId hoặc vehicleCode (mã QR).");
        }

        Booking matchedBooking = null;
        if ("RESERVED".equalsIgnoreCase(vehicle.getFleetStatus())) {
            matchedBooking = bookingRepository.findFirstByVehicleIdAndStatusOrderByIdDesc(vehicle.getId(), BookingStatus.CONFIRMED)
                    .orElse(null);
            if (matchedBooking != null && !matchedBooking.getUser().getId().equals(user.getId())) {
                throw new ConflictException("Phương tiện " + vehicle.getVehicleCode() + " đang được người dùng khác đặt giữ.");
            }
            if (matchedBooking != null) {
                matchedBooking.setStatus(BookingStatus.CHECKED_IN);
                bookingRepository.save(matchedBooking);
            }
        } else if (!"AVAILABLE".equalsIgnoreCase(vehicle.getFleetStatus())) {
            throw new ConflictException("Phương tiện " + vehicle.getVehicleCode() + " hiện không khả dụng (" + vehicle.getFleetStatus() + ").");
        }

        if (vehicle.getBatteryLevel() != null && vehicle.getBatteryLevel() < 15) {
            throw new BusinessRuleViolationException("Dung lượng pin quá thấp (" + vehicle.getBatteryLevel() + "%), không thể mở khóa.");
        }

        double startLat = request.getLatitude() != null ? request.getLatitude()
                : (vehicle.getLatitude() != null ? vehicle.getLatitude() : 10.8045);
        double startLng = request.getLongitude() != null ? request.getLongitude()
                : (vehicle.getLongitude() != null ? vehicle.getLongitude() : 106.7166);

        vehicle.setFleetStatus("IN_USE");
        vehicle.setLockStatus("UNLOCKED");
        vehicle.setLatitude(startLat);
        vehicle.setLongitude(startLng);
        vehicleRepository.save(vehicle);

        String sessionCode = "RIDE-" + vehicle.getVehicleCode() + "-" + System.currentTimeMillis() % 100000;
        LocalDateTime now = LocalDateTime.now();

        RideSession session = RideSession.builder()
                .sessionCode(sessionCode)
                .plate(vehicle.getPlate())
                .user(user)
                .vehicle(vehicle)
                .booking(matchedBooking)
                .timeIn(now)
                .status(RideSessionStatus.IN_PROGRESS)
                .startLatitude(startLat)
                .startLongitude(startLng)
                .distanceKm(0.0)
                .startBatteryLevel(vehicle.getBatteryLevel())
                .build();

        RideSession savedSession = rideSessionRepository.save(session);

        VehicleTelemetryLog initialLog = VehicleTelemetryLog.builder()
                .vehicle(vehicle)
                .rideSession(savedSession)
                .latitude(startLat)
                .longitude(startLng)
                .batteryLevel(vehicle.getBatteryLevel() != null ? vehicle.getBatteryLevel() : 100)
                .speedKmh(0.0)
                .lockStatus("UNLOCKED")
                .recordedAt(now)
                .build();
        telemetryLogRepository.save(initialLog);

        broadcastFleetSnapshot(vehicle, savedSession);
        return mapToRideSessionDTO(savedSession);
    }

    @Override
    public RideSessionResponseDTO endRide(Long sessionId, EndRideRequestDTO request) {
        RideSession session = rideSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyến đi với ID: " + sessionId));

        if (session.getTimeOut() != null) {
            return mapToRideSessionDTO(session);
        }

        Vehicle vehicle = vehicleRepository.findByIdForUpdate(session.getVehicle().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phương tiện của chuyến đi."));

        LocalDateTime now = LocalDateTime.now();
        double endLat = (request != null && request.getEndLatitude() != null) ? request.getEndLatitude()
                : (vehicle.getLatitude() != null ? vehicle.getLatitude() : 10.8045);
        double endLng = (request != null && request.getEndLongitude() != null) ? request.getEndLongitude()
                : (vehicle.getLongitude() != null ? vehicle.getLongitude() : 106.7166);

        if (vehicle.getLatitude() != null && vehicle.getLongitude() != null) {
            double extraKm = GeoUtils.haversineKm(vehicle.getLatitude(), vehicle.getLongitude(), endLat, endLng);
            if (extraKm > 0.005 && extraKm < 50.0) {
                double currentDist = session.getDistanceKm() != null ? session.getDistanceKm() : 0.0;
                session.setDistanceKm(Math.round((currentDist + extraKm) * 100.0) / 100.0);
            }
        }

        // Gán trạm gần nhất hoặc trạm chỉ định khi trả xe
        Long nearestStationId = findNearestStationId(endLat, endLng, request != null ? request.getEndStationId() : null);

        vehicle.setLatitude(endLat);
        vehicle.setLongitude(endLng);
        vehicle.setLockStatus("LOCKED");
        vehicle.setCurrentStationId(nearestStationId);
        int battery = vehicle.getBatteryLevel() != null ? vehicle.getBatteryLevel() : 100;
        vehicle.setFleetStatus(battery < 20 ? "LOW_BATTERY" : "AVAILABLE");
        vehicleRepository.save(vehicle);

        session.setTimeOut(now);
        session.setEndLatitude(endLat);
        session.setEndLongitude(endLng);
        session.setEndBatteryLevel(battery);
        BigDecimal totalFee = calculateRideFee(vehicle, session.getTimeIn(), now, session.getDistanceKm());
        session.setTotalFee(totalFee);
        // Trạng thái giữ IN_PROGRESS cho tới khi PaymentService hoàn tất thanh toán (hoặc Cash/VNPay IPN)
        RideSession updatedSession = rideSessionRepository.save(session);

        VehicleTelemetryLog endLog = VehicleTelemetryLog.builder()
                .vehicle(vehicle)
                .rideSession(updatedSession)
                .latitude(endLat)
                .longitude(endLng)
                .batteryLevel(battery)
                .speedKmh(0.0)
                .lockStatus("LOCKED")
                .recordedAt(now)
                .build();
        telemetryLogRepository.save(endLog);

        broadcastFleetSnapshot(vehicle, updatedSession);
        return mapToRideSessionDTO(updatedSession);
    }

    @Override
    @Transactional(readOnly = true)
    public RideSessionResponseDTO getRideSession(Long sessionId) {
        RideSession session = rideSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyến đi với ID: " + sessionId));
        return mapToRideSessionDTO(session);
    }

    @Override
    @Transactional(readOnly = true)
    public RideSessionResponseDTO getActiveRideForCurrentUser(Long userId) {
        User user = resolveUser(userId);
        return rideSessionRepository.findFirstByUser_IdAndStatusAndTimeOutIsNullOrderByTimeInDesc(user.getId(), RideSessionStatus.IN_PROGRESS)
                .map(this::mapToRideSessionDTO)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RideSessionResponseDTO> getRideHistory(Long userId) {
        User user = resolveUser(userId);
        return rideSessionRepository.findByUser_IdOrderByTimeInDesc(user.getId()).stream()
                .map(this::mapToRideSessionDTO)
                .collect(Collectors.toList());
    }

    private User resolveUser(Long explicitUserId) {
        if (explicitUserId != null) {
            return userRepository.findById(explicitUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản người dùng ID: " + explicitUserId));
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User principal) {
            return userRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản đăng nhập."));
        }
        return userRepository.findByUsername("user")
                .orElseThrow(() -> new ResourceNotFoundException("Không xác định được người dùng thực hiện giao dịch."));
    }

    private BigDecimal calculateRideFee(Vehicle vehicle, LocalDateTime timeIn, LocalDateTime timeOut, Double distanceKm) {
        PricingPolicy policy = resolvePricingPolicy(vehicle);
        BigDecimal unlockFee = policy != null ? policy.getBasePrice() : DEFAULT_UNLOCK_FEE;
        BigDecimal hourlyRate = policy != null ? policy.getExtraFeePerHour() : DEFAULT_HOURLY_RATE;

        long minutes = Math.max(1, Duration.between(timeIn, timeOut).toMinutes());
        // Tính theo block phút dựa trên đơn giá giờ (tối thiểu 1.000đ phí thời gian)
        BigDecimal timeFee = hourlyRate
                .multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(60), 0, RoundingMode.CEILING);

        double km = distanceKm != null ? distanceKm : 0.0;
        BigDecimal distanceFee = BigDecimal.valueOf(Math.round(km * 2000.0));

        return unlockFee.add(timeFee).add(distanceFee).setScale(2, RoundingMode.HALF_UP);
    }

    private PricingPolicy resolvePricingPolicy(Vehicle vehicle) {
        if (vehicle.getVehicleType() == null) {
            return null;
        }
        return pricingPolicyRepository
                .findFirstByVehicleType_IdAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                        vehicle.getVehicleType().getId(), LocalDateTime.now())
                .orElse(null);
    }

    private Long findNearestStationId(double lat, double lng, Long preferredStationId) {
        if (preferredStationId != null && stationRepository.existsById(preferredStationId)) {
            return preferredStationId;
        }
        return stationRepository.findAll().stream()
                .filter(b -> b.getLatitude() != null && b.getLongitude() != null)
                .min(Comparator.comparingDouble(b -> GeoUtils.haversineKm(lat, lng, b.getLatitude(), b.getLongitude())))
                .map(Station::getId)
                .orElse(null);
    }

    private FleetVehicleResponseDTO mapToFleetVehicleDTO(Vehicle v, Double distanceMeters, Map<Long, String> stationNames) {
        PricingPolicy policy = resolvePricingPolicy(v);
        return FleetVehicleResponseDTO.builder()
                .id(v.getId())
                .vehicleCode(v.getVehicleCode())
                .plate(v.getPlate())
                .brand(v.getBrand())
                .color(v.getColor())
                .vehicleTypeId(v.getVehicleType() != null ? v.getVehicleType().getId() : null)
                .vehicleTypeName(v.getVehicleType() != null ? v.getVehicleType().getTypeName() : null)
                .currentStationId(v.getCurrentStationId())
                .currentStationName(v.getCurrentStationId() != null ? stationNames.get(v.getCurrentStationId()) : "Đang lưu thông")
                .latitude(v.getLatitude())
                .longitude(v.getLongitude())
                .batteryLevel(v.getBatteryLevel())
                .lockStatus(v.getLockStatus())
                .fleetStatus(v.getFleetStatus())
                .distanceMeters(distanceMeters != null ? Math.round(distanceMeters * 10.0) / 10.0 : null)
                .unlockFee(policy != null ? policy.getBasePrice() : DEFAULT_UNLOCK_FEE)
                .hourlyRate(policy != null ? policy.getExtraFeePerHour() : DEFAULT_HOURLY_RATE)
                .build();
    }

    private RideSessionResponseDTO mapToRideSessionDTO(RideSession s) {
        Vehicle v = s.getVehicle();
        LocalDateTime effectiveEnd = s.getTimeOut() != null ? s.getTimeOut() : LocalDateTime.now();
        long durationMinutes = Math.max(1, Duration.between(s.getTimeIn(), effectiveEnd).toMinutes());
        BigDecimal estimated = s.getTotalFee() != null ? s.getTotalFee()
                : calculateRideFee(v, s.getTimeIn(), effectiveEnd, s.getDistanceKm());

        BigDecimal paidAmount = paymentCustomRepository.sumSuccessfulAmountBySessionId(s.getId());
        boolean isPaid = s.getStatus() == RideSessionStatus.COMPLETED
                || (s.getTotalFee() != null && paidAmount.compareTo(s.getTotalFee()) >= 0);

        return RideSessionResponseDTO.builder()
                .sessionId(s.getId())
                .sessionCode(s.getSessionCode())
                .userId(s.getUser() != null ? s.getUser().getId() : null)
                .username(s.getUser() != null ? s.getUser().getUsername() : null)
                .vehicleId(v != null ? v.getId() : null)
                .vehicleCode(v != null ? v.getVehicleCode() : null)
                .plate(s.getPlate())
                .vehicleBrand(v != null ? v.getBrand() : null)
                .vehicleTypeName(v != null && v.getVehicleType() != null ? v.getVehicleType().getTypeName() : null)
                .startLatitude(s.getStartLatitude())
                .startLongitude(s.getStartLongitude())
                .currentLatitude(v != null ? v.getLatitude() : null)
                .currentLongitude(v != null ? v.getLongitude() : null)
                .endLatitude(s.getEndLatitude())
                .endLongitude(s.getEndLongitude())
                .distanceKm(s.getDistanceKm() != null ? s.getDistanceKm() : 0.0)
                .currentBatteryLevel(v != null ? v.getBatteryLevel() : null)
                .lockStatus(v != null ? v.getLockStatus() : null)
                .startTime(s.getTimeIn().format(DATE_FORMAT))
                .endTime(s.getTimeOut() != null ? s.getTimeOut().format(DATE_FORMAT) : null)
                .durationMinutes(durationMinutes)
                .estimatedFee(estimated)
                .totalFee(s.getTotalFee())
                .status(s.getTimeOut() != null && !isPaid ? "AWAITING_PAYMENT" : s.getStatus().name())
                .paid(isPaid)
                .build();
    }

    private void broadcastFleetSnapshot(Vehicle v, RideSession session) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("vehicleId", v.getId());
        payload.put("vehicleCode", v.getVehicleCode());
        payload.put("latitude", v.getLatitude());
        payload.put("longitude", v.getLongitude());
        payload.put("batteryLevel", v.getBatteryLevel());
        payload.put("lockStatus", v.getLockStatus());
        payload.put("fleetStatus", v.getFleetStatus());
        payload.put("sessionId", session != null ? session.getId() : null);
        payload.put("timestamp", LocalDateTime.now().toString());
        sseEmitterManager.broadcastFleetEvent(payload);
    }
}
