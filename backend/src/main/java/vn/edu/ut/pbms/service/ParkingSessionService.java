package vn.edu.ut.pbms.service;

import vn.edu.ut.pbms.dto.response.ParkingSessionListResponseDTO;

/**
 * Service interface for Parking Session / Ride Session operations.
 */
public interface ParkingSessionService {

    /**
     * View parking sessions with dynamic filters.
     *
     * @param plate    the license plate to filter by (optional, exact match)
     * @param status   the status string to filter by (optional, validated against ParkingSessionStatus enum)
     * @param fromDate the minimum time_in as a string, cast to LocalDateTime (optional)
     * @return a response containing total_items and data array
     */
    ParkingSessionListResponseDTO getParkingSessions(String plate, String status, String fromDate);

    /**
     * Get parking sessions for a specific user.
     *
     * @param userId the user ID
     * @return a response containing total_items and data array
     */
    ParkingSessionListResponseDTO getParkingSessionsByUserId(Long userId);

    vn.edu.ut.pbms.dto.response.ParkingSessionResponseDTO updateSessionStatus(Long id, String status);

    void deleteSession(Long id);
}