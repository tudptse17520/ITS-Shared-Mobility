package vn.edu.ut.its.service;

import vn.edu.ut.its.dto.mobility.*;
import vn.edu.ut.its.dto.response.BookingResponseDTO;

import java.util.List;

public interface SharedMobilityService {

    List<StationResponseDTO> getStations();

    List<FleetVehicleResponseDTO> getAllFleetVehicles();

    List<FleetVehicleResponseDTO> getNearbyVehicles(Double lat, Double lng, Double radiusKm, Long vehicleTypeId, Boolean onlyAvailable);

    BookingResponseDTO reserveVehicle(ReserveVehicleRequestDTO request);

    void cancelReservation(Long vehicleId);

    RideSessionResponseDTO startRide(StartRideRequestDTO request);

    RideSessionResponseDTO endRide(Long sessionId, EndRideRequestDTO request);

    RideSessionResponseDTO getRideSession(Long sessionId);

    RideSessionResponseDTO getActiveRideForCurrentUser(Long userId);

    List<RideSessionResponseDTO> getRideHistory(Long userId);

    int releaseExpiredReservations();
}

