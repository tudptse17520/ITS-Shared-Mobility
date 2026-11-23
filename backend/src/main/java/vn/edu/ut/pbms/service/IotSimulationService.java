package vn.edu.ut.pbms.service;

import vn.edu.ut.pbms.dto.mobility.*;

import java.util.List;

public interface IotSimulationService {

    TelemetryLogResponseDTO ingestTelemetry(IotTelemetryRequestDTO request);

    RideSessionResponseDTO simulateRideStep(Long sessionId);

    FleetVehicleResponseDTO controlVehicle(Long vehicleId, IotControlRequestDTO request);

    List<TelemetryLogResponseDTO> getVehicleTelemetryHistory(Long vehicleId);
}

