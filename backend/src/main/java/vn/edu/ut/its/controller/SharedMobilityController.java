package vn.edu.ut.its.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.ut.its.dto.mobility.*;
import vn.edu.ut.its.dto.response.ApiResponse;
import vn.edu.ut.its.dto.response.BookingResponseDTO;
import vn.edu.ut.its.service.SharedMobilityService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mobility")
@RequiredArgsConstructor
@Tag(name = "Shared Mobility (ITS)", description = "API tìm xe gần nhất, đặt giữ xe chống trùng lặp, mở khóa QR và quản lý chuyến đi")
public class SharedMobilityController {

    private final SharedMobilityService sharedMobilityService;

    @Operation(summary = "Danh sách trạm xe công cộng")
    @GetMapping("/stations")
    public ResponseEntity<List<StationResponseDTO>> getStations() {
        return ResponseEntity.ok(sharedMobilityService.getStations());
    }

    @Operation(summary = "Danh sách toàn bộ đội xe")
    @GetMapping("/vehicles")
    public ResponseEntity<List<FleetVehicleResponseDTO>> getAllFleetVehicles() {
        return ResponseEntity.ok(sharedMobilityService.getAllFleetVehicles());
    }

    @Operation(summary = "Tìm xe khả dụng gần vị trí người dùng nhất (Haversine)")
    @GetMapping("/vehicles/nearby")
    public ResponseEntity<List<FleetVehicleResponseDTO>> getNearbyVehicles(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false, defaultValue = "15.0") Double radiusKm,
            @RequestParam(required = false) Long vehicleTypeId,
            @RequestParam(required = false, defaultValue = "false") Boolean onlyAvailable) {
        return ResponseEntity.ok(sharedMobilityService.getNearbyVehicles(lat, lng, radiusKm, vehicleTypeId, onlyAvailable));
    }

    @Operation(summary = "Đặt giữ xe trước (có Pessimistic Lock chống double-booking)")
    @PostMapping("/reserve")
    public ResponseEntity<BookingResponseDTO> reserveVehicle(@Valid @RequestBody ReserveVehicleRequestDTO request) {
        return ResponseEntity.ok(sharedMobilityService.reserveVehicle(request));
    }

    @Operation(summary = "Hủy giữ xe")
    @PostMapping("/vehicles/{vehicleId}/cancel-reserve")
    public ResponseEntity<ApiResponse<String>> cancelReservation(@PathVariable Long vehicleId) {
        sharedMobilityService.cancelReservation(vehicleId);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("Đã hủy giữ xe.")
                .build());
    }

    @Operation(summary = "Quét QR mở khóa xe và bắt đầu chuyến đi")
    @PostMapping("/rides/start")
    public ResponseEntity<RideSessionResponseDTO> startRide(@RequestBody StartRideRequestDTO request) {
        return ResponseEntity.ok(sharedMobilityService.startRide(request));
    }

    @Operation(summary = "Khóa xe, kết thúc chuyến đi và tính cước tự động")
    @PostMapping("/rides/{sessionId}/end")
    public ResponseEntity<RideSessionResponseDTO> endRide(
            @PathVariable Long sessionId,
            @RequestBody(required = false) EndRideRequestDTO request) {
        return ResponseEntity.ok(sharedMobilityService.endRide(sessionId, request));
    }

    @Operation(summary = "Chi tiết chuyến đi")
    @GetMapping("/rides/{sessionId}")
    public ResponseEntity<RideSessionResponseDTO> getRideSession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(sharedMobilityService.getRideSession(sessionId));
    }

    @Operation(summary = "Lấy chuyến đi đang diễn ra của người dùng")
    @GetMapping("/rides/active")
    public ResponseEntity<RideSessionResponseDTO> getActiveRide(@RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(sharedMobilityService.getActiveRideForCurrentUser(userId));
    }

    @Operation(summary = "Lịch sử chuyến đi của người dùng")
    @GetMapping("/rides/history")
    public ResponseEntity<List<RideSessionResponseDTO>> getRideHistory(@RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(sharedMobilityService.getRideHistory(userId));
    }
}

