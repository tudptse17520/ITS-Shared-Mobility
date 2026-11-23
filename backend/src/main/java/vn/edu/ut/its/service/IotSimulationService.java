package vn.edu.ut.its.service;

import vn.edu.ut.its.dto.mobility.*;

import java.util.List;

public interface IotSimulationService {

    TelemetryLogResponseDTO ingestTelemetry(IotTelemetryRequestDTO request);

    RideSessionResponseDTO simulateRideStep(Long sessionId);

    FleetVehicleResponseDTO controlVehicle(Long vehicleId, IotControlRequestDTO request);

    List<TelemetryLogResponseDTO> getVehicleTelemetryHistory(Long vehicleId);
}

