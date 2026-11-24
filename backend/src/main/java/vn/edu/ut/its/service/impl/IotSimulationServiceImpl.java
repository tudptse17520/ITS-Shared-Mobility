package vn.edu.ut.its.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ut.its.constant.RideSessionStatus;
import vn.edu.ut.its.dto.mobility.*;
import vn.edu.ut.its.entity.RideSession;
import vn.edu.ut.its.entity.Vehicle;
import vn.edu.ut.its.entity.VehicleTelemetryLog;
import vn.edu.ut.its.exception.BusinessRuleViolationException;
import vn.edu.ut.its.exception.ResourceNotFoundException;
import vn.edu.ut.its.repository.RideSessionRepository;
import vn.edu.ut.its.repository.UserRepository;
import vn.edu.ut.its.repository.VehicleRepository;
import vn.edu.ut.its.repository.VehicleTelemetryLogRepository;
import vn.edu.ut.its.security.SecurityUtils;
import vn.edu.ut.its.service.IotSimulationService;
import vn.edu.ut.its.service.SharedMobilityService;
import vn.edu.ut.its.service.SseEmitterManager;
import vn.edu.ut.its.util.GeoUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class IotSimulationServiceImpl implements IotSimulationService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final VehicleRepository vehicleRepository;
    private final RideSessionRepository rideSessionRepository;
    private final VehicleTelemetryLogRepository telemetryLogRepository;
    private final UserRepository userRepository;
    private final SharedMobilityService sharedMobilityService;
    private final SseEmitterManager sseEmitterManager;

    @Override
    public TelemetryLogResponseDTO ingestTelemetry(IotTelemetryRequestDTO request) {
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phương tiện với ID: " + request.getVehicleId()));

        double prevLat = vehicle.getLatitude() != null ? vehicle.getLatitude() : request.getLatitude();
        double prevLng = vehicle.getLongitude() != null ? vehicle.getLongitude() : request.getLongitude();
        double stepKm = GeoUtils.haversineKm(prevLat, prevLng, request.getLatitude(), request.getLongitude());

        vehicle.setLatitude(request.getLatitude());
        vehicle.setLongitude(request.getLongitude());

        int currentBattery = vehicle.getBatteryLevel() != null ? vehicle.getBatteryLevel() : 100;
        int newBattery;
        if (request.getBatteryLevel() != null) {
            newBattery = Math.max(0, Math.min(100, request.getBatteryLevel()));
        } else {
            int drain = stepKm > 0.05 ? Math.max(1, (int) Math.round(stepKm * 3)) : 0;
            newBattery = Math.max(5, currentBattery - drain);
        }
        vehicle.setBatteryLevel(newBattery);

        if (request.getLockStatus() != null && !request.getLockStatus().isBlank()) {
            vehicle.setLockStatus(request.getLockStatus().trim().toUpperCase());
        }

        if (newBattery < 15 && "AVAILABLE".equalsIgnoreCase(vehicle.getFleetStatus())) {
            vehicle.setFleetStatus("LOW_BATTERY");
        }
        vehicleRepository.save(vehicle);

        RideSession activeSession = rideSessionRepository
                .findFirstByVehicle_IdAndStatusAndEndedAtIsNullOrderByStartedAtDesc(vehicle.getId(), RideSessionStatus.IN_PROGRESS)
                .orElse(null);

        if (activeSession != null && stepKm > 0.001 && stepKm < 50.0) {
            double totalDist = (activeSession.getDistanceKm() != null ? activeSession.getDistanceKm() : 0.0) + stepKm;
            activeSession.setDistanceKm(Math.round(totalDist * 100.0) / 100.0);
            rideSessionRepository.save(activeSession);
        }

        VehicleTelemetryLog logEntry = VehicleTelemetryLog.builder()
                .vehicle(vehicle)
                .rideSession(activeSession)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .batteryLevel(newBattery)
                .speedKmh(request.getSpeedKmh() != null ? request.getSpeedKmh() : 18.5)
                .lockStatus(vehicle.getLockStatus())
                .recordedAt(LocalDateTime.now())
                .build();

        VehicleTelemetryLog saved = telemetryLogRepository.save(logEntry);
        broadcastTelemetry(vehicle, activeSession, saved.getSpeedKmh());
        return mapToDTO(saved);
    }

    @Override
    public RideSessionResponseDTO simulateRideStep(Long sessionId) {
        RideSession session = rideSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyến đi ID: " + sessionId));

        SecurityUtils.assertAdminOrOwner(
                session.getUser() != null ? session.getUser().getId() : null,
                userRepository,
                "Bạn không có quyền mô phỏng GPS cho chuyến đi của người dùng khác.");

        if (session.getEndedAt() != null) {
            throw new BusinessRuleViolationException("Chuyến đi đã kết thúc, không thể mô phỏng di chuyển tiếp.");
        }

        Vehicle vehicle = session.getVehicle();
        double currentLat = vehicle.getLatitude() != null ? vehicle.getLatitude() : 10.8045;
        double currentLng = vehicle.getLongitude() != null ? vehicle.getLongitude() : 106.7166;

        // Dịch chuyển ngẫu nhiên có định hướng về phía trung tâm TP.HCM (~250m - 400m mỗi bước)
        double deltaLat = ThreadLocalRandom.current().nextDouble(-0.0022, 0.0026);
        double deltaLng = ThreadLocalRandom.current().nextDouble(-0.0022, 0.0026);
        if (Math.abs(deltaLat) < 0.0008 && Math.abs(deltaLng) < 0.0008) {
            deltaLat = 0.0018;
            deltaLng = 0.0015;
        }

        double nextLat = Math.round((currentLat + deltaLat) * 1000000.0) / 1000000.0;
        double nextLng = Math.round((currentLng + deltaLng) * 1000000.0) / 1000000.0;

        int currentBat = vehicle.getBatteryLevel() != null ? vehicle.getBatteryLevel() : 100;
        int nextBat = Math.max(10, currentBat - ThreadLocalRandom.current().nextInt(1, 3));
        double speed = Math.round(ThreadLocalRandom.current().nextDouble(16.0, 32.0) * 10.0) / 10.0;

        ingestTelemetry(IotTelemetryRequestDTO.builder()
                .vehicleId(vehicle.getId())
                .latitude(nextLat)
                .longitude(nextLng)
                .batteryLevel(nextBat)
                .speedKmh(speed)
                .lockStatus("UNLOCKED")
                .build());

        return sharedMobilityService.getRideSession(sessionId);
    }

    @Override
    public FleetVehicleResponseDTO controlVehicle(Long vehicleId, IotControlRequestDTO request) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phương tiện với ID: " + vehicleId));

        String cmd = request.getCommand().trim().toUpperCase();
        switch (cmd) {
            case "UNLOCK" -> vehicle.setLockStatus("UNLOCKED");
            case "LOCK" -> vehicle.setLockStatus("LOCKED");
            case "RECHARGE_100" -> {
                vehicle.setBatteryLevel(100);
                if ("LOW_BATTERY".equalsIgnoreCase(vehicle.getFleetStatus())) {
                    vehicle.setFleetStatus("AVAILABLE");
                }
            }
            case "SET_BATTERY" -> {
                int bat = request.getBatteryLevel() != null ? Math.max(0, Math.min(100, request.getBatteryLevel())) : 50;
                vehicle.setBatteryLevel(bat);
                if (bat < 15 && "AVAILABLE".equalsIgnoreCase(vehicle.getFleetStatus())) {
                    vehicle.setFleetStatus("LOW_BATTERY");
                } else if (bat >= 20 && "LOW_BATTERY".equalsIgnoreCase(vehicle.getFleetStatus())) {
                    vehicle.setFleetStatus("AVAILABLE");
                }
            }
            case "MAINTENANCE" -> {
                vehicle.setFleetStatus("MAINTENANCE");
                vehicle.setLockStatus("LOCKED");
            }
            case "SET_AVAILABLE" -> {
                vehicle.setFleetStatus("AVAILABLE");
                vehicle.setLockStatus("LOCKED");
            }
            case "MOVE_GPS" -> {
                if (request.getLatitude() != null && request.getLongitude() != null) {
                    vehicle.setLatitude(request.getLatitude());
                    vehicle.setLongitude(request.getLongitude());
                }
            }
            default -> throw new BusinessRuleViolationException("Lệnh điều khiển IoT không hợp lệ: " + cmd);
        }

        vehicleRepository.save(vehicle);

        telemetryLogRepository.save(VehicleTelemetryLog.builder()
                .vehicle(vehicle)
                .latitude(vehicle.getLatitude() != null ? vehicle.getLatitude() : 10.8045)
                .longitude(vehicle.getLongitude() != null ? vehicle.getLongitude() : 106.7166)
                .batteryLevel(vehicle.getBatteryLevel() != null ? vehicle.getBatteryLevel() : 100)
                .speedKmh(0.0)
                .lockStatus(vehicle.getLockStatus())
                .recordedAt(LocalDateTime.now())
                .build());

        broadcastTelemetry(vehicle, null, 0.0);

        return sharedMobilityService.getAllFleetVehicles().stream()
                .filter(v -> v.getId().equals(vehicleId))
                .findFirst()
                .orElseThrow();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TelemetryLogResponseDTO> getVehicleTelemetryHistory(Long vehicleId) {
        return telemetryLogRepository.findTop50ByVehicle_IdOrderByRecordedAtDesc(vehicleId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private TelemetryLogResponseDTO mapToDTO(VehicleTelemetryLog log) {
        return TelemetryLogResponseDTO.builder()
                .id(log.getId())
                .vehicleId(log.getVehicle().getId())
                .vehicleCode(log.getVehicle().getVehicleCode())
                .sessionId(log.getRideSession() != null ? log.getRideSession().getId() : null)
                .latitude(log.getLatitude())
                .longitude(log.getLongitude())
                .batteryLevel(log.getBatteryLevel())
                .speedKmh(log.getSpeedKmh())
                .lockStatus(log.getLockStatus())
                .recordedAt(log.getRecordedAt().format(FORMATTER))
                .build();
    }

    private void broadcastTelemetry(Vehicle v, RideSession session, Double speedKmh) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("vehicleId", v.getId());
        payload.put("vehicleCode", v.getVehicleCode());
        payload.put("latitude", v.getLatitude());
        payload.put("longitude", v.getLongitude());
        payload.put("batteryLevel", v.getBatteryLevel());
        payload.put("lockStatus", v.getLockStatus());
        payload.put("fleetStatus", v.getFleetStatus());
        payload.put("speedKmh", speedKmh);
        payload.put("sessionId", session != null ? session.getId() : null);
        payload.put("distanceKm", session != null ? session.getDistanceKm() : null);
        payload.put("timestamp", LocalDateTime.now().toString());
        sseEmitterManager.broadcastFleetEvent(payload);
    }
}

