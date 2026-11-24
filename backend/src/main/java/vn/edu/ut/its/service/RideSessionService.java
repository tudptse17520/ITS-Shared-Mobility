package vn.edu.ut.its.service;

import vn.edu.ut.its.dto.response.RideSessionListResponseDTO;

/**
 * Service interface for Ride Session operations.
 */
public interface RideSessionService {

    /**
     * View ride sessions with dynamic filters.
     *
     * @param plate    the license plate to filter by (optional, exact match)
     * @param status   the status string to filter by (optional, validated against RideSessionStatus enum)
     * @param fromDate the minimum started_at as a string, cast to LocalDateTime (optional)
     * @return a response containing total_items and data array
     */
    RideSessionListResponseDTO getRideSessions(String plate, String status, String fromDate);

    /**
     * Get ride sessions for a specific user.
     *
     * @param userId the user ID
     * @return a response containing total_items and data array
     */
    RideSessionListResponseDTO getRideSessionsByUserId(Long userId);

    vn.edu.ut.its.dto.response.RideSessionDetailDTO updateSessionStatus(Long id, String status);

    void deleteSession(Long id);
}