package vn.edu.ut.pbms.service.impl;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.edu.ut.pbms.constant.BookingStatus;
import vn.edu.ut.pbms.constant.ParkingSlotStatus;
import vn.edu.ut.pbms.dto.request.BookingRequestDTO;
import vn.edu.ut.pbms.dto.response.BookingListResponseDTO;
import vn.edu.ut.pbms.dto.response.BookingResponseDTO;
import vn.edu.ut.pbms.entity.Booking;
import vn.edu.ut.pbms.entity.ParkingSlot;
import vn.edu.ut.pbms.entity.User;
import vn.edu.ut.pbms.entity.Vehicle;
import vn.edu.ut.pbms.exception.BusinessRuleViolationException;
import vn.edu.ut.pbms.exception.ResourceNotFoundException;
import vn.edu.ut.pbms.repository.BookingRepository;
import vn.edu.ut.pbms.repository.ParkingSlotRepository;
import vn.edu.ut.pbms.repository.UserRepository;
import vn.edu.ut.pbms.repository.VehicleRepository;
import vn.edu.ut.pbms.service.BookingService;
import vn.edu.ut.pbms.service.SlotAvailabilityService;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final SlotAvailabilityService slotAvailabilityService;

    private User resolveUser(Long explicitUserId) {
        if (explicitUserId != null && explicitUserId > 0 && userRepository.existsById(explicitUserId)) {
            return userRepository.findById(explicitUserId).get();
        }
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User u) {
            return userRepository.findById(u.getId()).orElse(u);
        }
        return userRepository.findByUsername("user")
                .orElseGet(() -> userRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản người dùng hợp lệ.")));
    }

    @Override
    public BookingResponseDTO createBooking(BookingRequestDTO request) {
        // 1. Validate User from JWT or fallback to default customer user
        User user = resolveUser(null);

        // 2. Validate Vehicle
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phương tiện với ID: " + request.getVehicleId()));

        // 3. Validate Parking Slot
        ParkingSlot slot = parkingSlotRepository.findById(request.getParkingSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vị trí đỗ với ID: " + request.getParkingSlotId()));

        // Check slot availability
        if (slot.getStatus() != ParkingSlotStatus.AVAILABLE) {
            throw new BusinessRuleViolationException("Vị trí đỗ '" + slot.getSlotName() + "' hiện tại không khả dụng để đặt chỗ.");
        }

        // 4. Parse Dates
        LocalDateTime expectedTimeIn = parseDateTime(request.getExpectedTimeIn(), "Thời gian dự kiến vào");
        LocalDateTime expectedTimeOut = parseDateTime(request.getExpectedTimeOut(), "Thời gian dự kiến ra");

        if (expectedTimeOut.isBefore(expectedTimeIn)) {
            throw new BusinessRuleViolationException("Thời gian dự kiến ra không được trước thời gian dự kiến vào.");
        }

        // 5. Build and save Booking
        Booking booking = Booking.builder()
                .user(user)
                .vehicle(vehicle)
                .parkingSlot(slot)
                .expectedTimeIn(expectedTimeIn)
                .expectedTimeOut(expectedTimeOut)
                .status(BookingStatus.PENDING)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        // 6. Update Slot status to RESERVED to hold the slot
        slotAvailabilityService.updateSlotStatus(slot.getId(), ParkingSlotStatus.RESERVED);

        return BookingResponseDTO.builder()
                .id(savedBooking.getId())
                .status(savedBooking.getStatus())
                .message("Tạo đơn đặt chỗ thành công. Trạng thái: PENDING.")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingListResponseDTO> getBookingsByUserId(Long userId) {
        User targetUser = resolveUser(userId);
        List<Booking> bookings = bookingRepository.findByUserId(targetUser.getId());

        return bookings.stream()
                .map(this::mapToBookingListResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingListResponseDTO> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(this::mapToBookingListResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đặt chỗ với ID: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessRuleViolationException("Chỉ có thể hủy đặt chỗ khi đang ở trạng thái PENDING hoặc CONFIRMED.");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        if (booking.getParkingSlot() != null) {
            slotAvailabilityService.updateSlotStatus(booking.getParkingSlot().getId(), ParkingSlotStatus.AVAILABLE);
        }
    }

    @Override
    public BookingListResponseDTO updateBookingStatus(Long bookingId, BookingStatus status) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đặt chỗ với ID: " + bookingId));

        booking.setStatus(status);
        Booking saved = bookingRepository.save(booking);

        if (booking.getParkingSlot() != null) {
            if (status == BookingStatus.CANCELLED || status == BookingStatus.EXPIRED) {
                slotAvailabilityService.updateSlotStatus(booking.getParkingSlot().getId(), ParkingSlotStatus.AVAILABLE);
            } else if (status == BookingStatus.CHECKED_IN) {
                slotAvailabilityService.updateSlotStatus(booking.getParkingSlot().getId(), ParkingSlotStatus.OCCUPIED);
            } else {
                slotAvailabilityService.updateSlotStatus(booking.getParkingSlot().getId(), ParkingSlotStatus.RESERVED);
            }
        }

        return mapToBookingListResponseDTO(saved);
    }

    @Override
    public void deleteBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đặt chỗ với ID: " + bookingId));

        Long slotId = booking.getParkingSlot() != null ? booking.getParkingSlot().getId() : null;
        boolean wasReserved = booking.getParkingSlot() != null
                && booking.getParkingSlot().getStatus() == ParkingSlotStatus.RESERVED;

        try {
            bookingRepository.delete(booking);
            bookingRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new BusinessRuleViolationException("Không thể xóa đặt chỗ đã phát sinh giao dịch thanh toán liên quan.");
        }

        if (slotId != null && wasReserved) {
            slotAvailabilityService.updateSlotStatus(slotId, ParkingSlotStatus.AVAILABLE);
        }
    }

    private BookingListResponseDTO mapToBookingListResponseDTO(Booking booking) {
        return BookingListResponseDTO.builder()
                .bookingId(booking.getId())
                .parkingSlotId(booking.getParkingSlot() != null ? booking.getParkingSlot().getId() : null)
                .parkingSlotName(booking.getParkingSlot() != null ? booking.getParkingSlot().getSlotName() : null)
                .expectedTimeIn(booking.getExpectedTimeIn() != null ? booking.getExpectedTimeIn().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .expectedTimeOut(booking.getExpectedTimeOut() != null ? booking.getExpectedTimeOut().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .customerName(booking.getUser() != null ? booking.getUser().getFullName() : null)
                .customerPhone(booking.getUser() != null ? booking.getUser().getPhoneNumber() : null)
                .licensePlate(booking.getVehicle() != null ? booking.getVehicle().getPlate() : null)
                .vehicleTypeName(booking.getVehicle() != null && booking.getVehicle().getVehicleType() != null ? booking.getVehicle().getVehicleType().getTypeName() : null)
                .status(booking.getStatus())
                .build();
    }

    private LocalDateTime parseDateTime(String text, String fieldName) {
        if (text == null || text.trim().isEmpty()) {
            throw new BusinessRuleViolationException(fieldName + " không được để trống.");
        }
        try {
            return LocalDateTime.parse(text.trim());
        } catch (DateTimeParseException e1) {
            try {
                return ZonedDateTime.parse(text.trim()).toLocalDateTime();
            } catch (DateTimeParseException e2) {
                try {
                    // Try pattern yyyy-MM-dd HH:mm:ss
                    return LocalDateTime.parse(text.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } catch (DateTimeParseException e3) {
                    throw new BusinessRuleViolationException(
                            "Định dạng " + fieldName + " không hợp lệ. Vui lòng sử dụng định dạng ISO (yyyy-MM-ddTHH:mm:ss) hoặc yyyy-MM-dd HH:mm:ss");
                }
            }
        }
    }
}
