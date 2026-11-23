package vn.edu.ut.its.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ut.its.entity.Vehicle;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Vehicle entity.
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByPlate(String plate);

    boolean existsByPlate(String plate);

    List<Vehicle> findByUserId(Long userId);

    Optional<Vehicle> findByVehicleCode(String vehicleCode);

    List<Vehicle> findByVehicleCodeIsNotNull();

    List<Vehicle> findByFleetStatus(String fleetStatus);

    /**
     * Pessimistic Write Lock để xử lý cạnh tranh đồng thời (Concurrency Control)
     * khi nhiều người dùng cùng đặt giữ hoặc mở khóa 1 chiếc xe tại cùng thời điểm.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Vehicle v WHERE v.id = :id")
    Optional<Vehicle> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Vehicle v WHERE v.vehicleCode = :vehicleCode")
    Optional<Vehicle> findByVehicleCodeForUpdate(@Param("vehicleCode") String vehicleCode);
}
