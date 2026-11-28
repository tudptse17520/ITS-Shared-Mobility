package vn.edu.ut.its.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ut.its.constant.BookingStatus;
import vn.edu.ut.its.entity.Booking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByStatusAndExpiredAtBefore(BookingStatus status, LocalDateTime now);

    List<Booking> findByUserId(Long userId);

    Optional<Booking> findFirstByVehicleIdAndStatusOrderByIdDesc(Long vehicleId, BookingStatus status);
}
