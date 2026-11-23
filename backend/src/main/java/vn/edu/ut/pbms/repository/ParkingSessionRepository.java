package vn.edu.ut.pbms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ut.pbms.constant.ParkingSessionStatus;
import vn.edu.ut.pbms.entity.ParkingSession;

/**
 * Repository for ParkingSession entity.
 * Provides query method for VehicleType deactivation check (E3).
 */
@Repository
public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {

    /**
     * Check if any parking session with the given status exists for vehicles of a specific vehicle type.
     * Uses Spring Data JPA derived query through nested property: vehicle -> vehicleType -> id.
     *
     * @param vehicleTypeId the vehicle type ID to check
     * @param status        the parking session status to match (IN_PROGRESS)
     * @return true if at least one active parking session exists for vehicles of this type
     */
    boolean existsByVehicle_VehicleType_IdAndStatus(Long vehicleTypeId, ParkingSessionStatus status);

    java.util.Optional<ParkingSession> findFirstByVehicle_IdAndStatusOrderByTimeInDesc(Long vehicleId, ParkingSessionStatus status);

    java.util.List<ParkingSession> findByUser_IdOrderByTimeInDesc(Long userId);

    java.util.Optional<ParkingSession> findFirstByUser_IdAndStatusOrderByTimeInDesc(Long userId, ParkingSessionStatus status);
}
