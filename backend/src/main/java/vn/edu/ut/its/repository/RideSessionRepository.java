package vn.edu.ut.its.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ut.its.constant.RideSessionStatus;
import vn.edu.ut.its.entity.RideSession;

/**
 * Repository for RideSession entity.
 * Provides query method for VehicleType deactivation check (E3).
 */
@Repository
public interface RideSessionRepository extends JpaRepository<RideSession, Long> {

    /**
     * Check if any ride session with the given status exists for vehicles of a specific vehicle type.
     * Uses Spring Data JPA derived query through nested property: vehicle -> vehicleType -> id.
     *
     * @param vehicleTypeId the vehicle type ID to check
     * @param status        the ride session status to match (IN_PROGRESS)
     * @return true if at least one active ride session exists for vehicles of this type
     */
    boolean existsByVehicle_VehicleType_IdAndStatusAndEndedAtIsNull(Long vehicleTypeId, RideSessionStatus status);

    java.util.Optional<RideSession> findFirstByVehicle_IdAndStatusAndEndedAtIsNullOrderByStartedAtDesc(Long vehicleId, RideSessionStatus status);

    java.util.List<RideSession> findByUser_IdOrderByStartedAtDesc(Long userId);

    java.util.Optional<RideSession> findFirstByUser_IdAndStatusAndEndedAtIsNullOrderByStartedAtDesc(Long userId, RideSessionStatus status);
}
