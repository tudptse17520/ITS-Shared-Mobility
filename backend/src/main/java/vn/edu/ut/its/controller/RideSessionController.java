package vn.edu.ut.its.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import vn.edu.ut.its.dto.response.RideSessionListResponseDTO;
import vn.edu.ut.its.service.RideSessionService;

/**
 * REST Controller for Ride Sessions management.
 * Endpoint base: /api/v1/ride-sessions
 */
@RestController
@RequestMapping("/api/v1/ride-sessions")
@RequiredArgsConstructor
@CrossOrigin
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "Ride Session", description = "Các API tra cứu và quản lý chuyến đi")
public class RideSessionController {

    private final RideSessionService rideSessionService;

    /**
     * Tra cứu chuyến đi theo biển số, trạng thái và ngày bắt đầu.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RideSessionListResponseDTO> getRideSessions(
            @RequestParam(required = false) String plate,
            @RequestParam(required = false) String status,
            @RequestParam(name = "fromDate", required = false) String fromDate) {

        RideSessionListResponseDTO response = rideSessionService.getRideSessions(plate, status, fromDate);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy lịch sử chuyến đi của người dùng.
     */
    @GetMapping("/users/{userId}/sessions")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<RideSessionListResponseDTO> getUserSessions(@PathVariable("userId") Long userId) {
        RideSessionListResponseDTO response = rideSessionService.getRideSessionsByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<vn.edu.ut.its.dto.response.RideSessionDetailDTO> updateSessionStatus(
            @PathVariable("id") Long id,
            @RequestBody java.util.Map<String, String> body) {
        return ResponseEntity.ok(rideSessionService.updateSessionStatus(id, body.get("status")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSession(@PathVariable("id") Long id) {
        rideSessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }
}
