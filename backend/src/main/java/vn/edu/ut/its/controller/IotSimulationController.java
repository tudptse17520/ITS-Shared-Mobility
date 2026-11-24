package vn.edu.ut.its.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import vn.edu.ut.its.dto.mobility.*;
import vn.edu.ut.its.service.IotSimulationService;
import vn.edu.ut.its.service.SseEmitterManager;

import java.util.List;

@RestController
@RequestMapping("/api/v1/iot")
@RequiredArgsConstructor
@Tag(name = "IoT Simulation (ITS)", description = "API giả lập bản tin GPS, % pin, đóng/mở khóa và SSE stream thời gian thực")
public class IotSimulationController {

    private final IotSimulationService iotSimulationService;
    private final SseEmitterManager sseEmitterManager;

    @Operation(summary = "Nhận bản tin giả lập IoT (GPS, % pin, trạng thái khóa)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/telemetry")
    public ResponseEntity<TelemetryLogResponseDTO> ingestTelemetry(@Valid @RequestBody IotTelemetryRequestDTO request) {
        return ResponseEntity.ok(iotSimulationService.ingestTelemetry(request));
    }

    @Operation(summary = "Mô phỏng 1 bước xe di chuyển GPS trong chuyến đi")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/simulate-step/{sessionId}")
    public ResponseEntity<RideSessionResponseDTO> simulateRideStep(@PathVariable Long sessionId) {
        return ResponseEntity.ok(iotSimulationService.simulateRideStep(sessionId));
    }

    @Operation(summary = "Điều khiển trạng thái xe từ xa (LOCK, UNLOCK, RECHARGE_100, MAINTENANCE...)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/vehicles/{vehicleId}/control")
    public ResponseEntity<FleetVehicleResponseDTO> controlVehicle(
            @PathVariable Long vehicleId,
            @Valid @RequestBody IotControlRequestDTO request) {
        return ResponseEntity.ok(iotSimulationService.controlVehicle(vehicleId, request));
    }

    @Operation(summary = "Lịch sử bản tin telemetry của phương tiện")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/telemetry/{vehicleId}")
    public ResponseEntity<List<TelemetryLogResponseDTO>> getVehicleTelemetryHistory(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(iotSimulationService.getVehicleTelemetryHistory(vehicleId));
    }

    @Operation(summary = "SSE Stream nhận cập nhật vị trí GPS và trạng thái đội xe thời gian thực")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamFleetTelemetry() {
        return sseEmitterManager.subscribe(null);
    }
}

