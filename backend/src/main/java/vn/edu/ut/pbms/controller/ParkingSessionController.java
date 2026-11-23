package vn.edu.ut.pbms.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import vn.edu.ut.pbms.dto.response.ParkingSessionListResponseDTO;
import vn.edu.ut.pbms.service.ParkingSessionService;

/**
 * REST Controller for Parking Sessions management.
 * Endpoint base: /api/v1/sessions
 */
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@CrossOrigin
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "Parking Session", description = "Các API quản lý lượt gửi xe")
public class ParkingSessionController {

    private final ParkingSessionService parkingSessionService;

    /**
     * API: Lấy danh sách lượt gửi xe (Parking Sessions) với bộ lọc động.
     * Staff dùng để tìm xe lúc ra, Manager dùng để "Theo dõi xe quá giờ"[cite: 139].
     *
     * @param plate    (Tùy chọn) Tìm kiếm chính xác theo biển số [cite: 142]
     * @param status   (Tùy chọn) Trạng thái lượt gửi: IN_PROGRESS (đang trong bãi) / COMPLETED (đã ra) [cite: 142]
     * @param fromDate (Tùy chọn) Lọc thời gian vào bãi từ ngày... [cite: 142]
     * @return HTTP 200 với total_items và mảng data chi tiết [cite: 144]
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ParkingSessionListResponseDTO> getParkingSessions(
            @RequestParam(required = false) String plate,
            @RequestParam(required = false) String status,
            @RequestParam(name = "fromDate", required = false) String fromDate) {

        ParkingSessionListResponseDTO response = parkingSessionService.getParkingSessions(plate, status, fromDate);
        return ResponseEntity.ok(response);
    }

    /**
     * API: Lấy danh sách lượt gửi xe của người dùng cá nhân.
     */
    @GetMapping("/users/{userId}/sessions")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<ParkingSessionListResponseDTO> getUserSessions(@PathVariable("userId") Long userId) {
        ParkingSessionListResponseDTO response = parkingSessionService.getParkingSessionsByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<vn.edu.ut.pbms.dto.response.ParkingSessionResponseDTO> updateSessionStatus(
            @PathVariable("id") Long id,
            @RequestBody java.util.Map<String, String> body) {
        return ResponseEntity.ok(parkingSessionService.updateSessionStatus(id, body.get("status")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSession(@PathVariable("id") Long id) {
        parkingSessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }
}