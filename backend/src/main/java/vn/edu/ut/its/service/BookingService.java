package vn.edu.ut.its.service;

import java.util.List;
import vn.edu.ut.its.constant.BookingStatus;
import vn.edu.ut.its.dto.request.BookingRequestDTO;
import vn.edu.ut.its.dto.response.BookingListResponseDTO;
import vn.edu.ut.its.dto.response.BookingResponseDTO;

public interface BookingService {
    BookingResponseDTO createBooking(BookingRequestDTO request);
    List<BookingListResponseDTO> getBookingsByUserId(Long userId);
    List<BookingListResponseDTO> getAllBookings();
    void cancelBooking(Long bookingId);
    BookingListResponseDTO updateBookingStatus(Long bookingId, BookingStatus status);
    void deleteBooking(Long bookingId);
}
