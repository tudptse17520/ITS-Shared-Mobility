package vn.edu.ut.pbms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ut.pbms.entity.VehicleTelemetryLog;

import java.util.List;

@Repository
public interface VehicleTelemetryLogRepository extends JpaRepository<VehicleTelemetryLog, Long> {

    List<VehicleTelemetryLog> findTop50ByVehicle_IdOrderByRecordedAtDesc(Long vehicleId);

    List<VehicleTelemetryLog> findByParkingSession_IdOrderByRecordedAtAsc(Long parkingSessionId);
}

