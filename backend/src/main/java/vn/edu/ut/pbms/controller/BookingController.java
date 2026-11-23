package vn.edu.ut.pbms.controller;

import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import vn.edu.ut.pbms.constant.BookingStatus;
import vn.edu.ut.pbms.dto.request.BookingRequestDTO;
import vn.edu.ut.pbms.dto.response.BookingListResponseDTO;
import vn.edu.ut.pbms.dto.response.BookingResponseDTO;
import vn.edu.ut.pbms.service.BookingService;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "Booking", description = "API quản lý đặt giữ & chuyến đi chia sẻ")
public class BookingController {

    private final BookingService bookingService;

    @Operation(summary = "Đặt chỗ đỗ trước từ xa")
    @PostMapping("/bookings")
    public ResponseEntity<BookingResponseDTO> createBooking(
            @Valid @RequestBody BookingRequestDTO request) {
        BookingResponseDTO response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Kiểm tra lịch sử đặt giữ cá nhân của người dùng")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    @GetMapping("/users/{userId}/bookings")
    public ResponseEntity<List<BookingListResponseDTO>> getUserBookings(
            @PathVariable("userId") Long userId) {
        List<BookingListResponseDTO> response = bookingService.getBookingsByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Xem danh sách toàn bộ lượt đặt giữ (Dành cho Admin)")
    @GetMapping("/bookings")
    public ResponseEntity<List<BookingListResponseDTO>> getAllBookings() {
        List<BookingListResponseDTO> response = bookingService.getAllBookings();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Hủy lượt đặt giữ")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PutMapping("/bookings/{bookingId}/cancel")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable("bookingId") Long bookingId) {
        bookingService.cancelBooking(bookingId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Cập nhật trạng thái đặt giữ (Admin)")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/bookings/{bookingId}/status")
    public ResponseEntity<BookingListResponseDTO> updateBookingStatus(
            @PathVariable("bookingId") Long bookingId,
            @RequestBody Map<String, String> payload) {
        BookingStatus status = BookingStatus.valueOf(payload.get("status").trim().toUpperCase());
        return ResponseEntity.ok(bookingService.updateBookingStatus(bookingId, status));
    }

    @Operation(summary = "Xóa đặt giữ (Admin)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/bookings/{bookingId}")
    public ResponseEntity<Void> deleteBooking(@PathVariable("bookingId") Long bookingId) {
        bookingService.deleteBooking(bookingId);
        return ResponseEntity.noContent().build();
    }
}
