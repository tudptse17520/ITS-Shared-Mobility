package vn.edu.ut.its.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ut.its.entity.VehicleTelemetryLog;

import java.util.List;

@Repository
public interface VehicleTelemetryLogRepository extends JpaRepository<VehicleTelemetryLog, Long> {

    List<VehicleTelemetryLog> findTop50ByVehicle_IdOrderByRecordedAtDesc(Long vehicleId);

}

