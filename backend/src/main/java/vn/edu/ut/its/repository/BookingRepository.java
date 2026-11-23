package vn.edu.ut.its.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ut.its.entity.Booking;

/**
 * Repository interface for Booking entity.
 */
import java.util.List;
import java.util.Optional;
import vn.edu.ut.its.constant.BookingStatus;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByVehicleIdAndStatus(Long vehicleId, BookingStatus status);

    List<Booking> findByUserId(Long userId);

    Optional<Booking> findFirstByVehicleIdAndStatusOrderByIdDesc(Long vehicleId, BookingStatus status);

    boolean existsByStationDockId(Long stationDockId);
}
