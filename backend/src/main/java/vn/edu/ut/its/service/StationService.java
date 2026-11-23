package vn.edu.ut.its.service;

import vn.edu.ut.its.constant.StationStatus;
import vn.edu.ut.its.dto.request.StationRequestDTO;
import vn.edu.ut.its.dto.request.StationStatusRequestDTO;
import vn.edu.ut.its.dto.response.StationListResponseDTO;
import vn.edu.ut.its.dto.response.StationActionResponseDTO;

/**
 * Service interface for Station business logic.
 * Defines all operations for managing stations/stations.
 */
public interface StationService {

    /**
     * Retrieve a list of stations filtered by keyword and status.
     *
     * @param keyword the keyword to search (optional)
     * @param status  the status to filter by (optional)
     * @return a response containing a message and list of station details
     */
    StationListResponseDTO getStations(String keyword, StationStatus status);

    /**
     * Create a new station.
     *
     * @param requestDTO the station data from the client
     * @return the created station response with id and message
     */
    StationActionResponseDTO createStation(StationRequestDTO requestDTO);

    /**
     * Update an existing station.
     *
     * @param id         the ID of the station to update
     * @param requestDTO the updated station data
     * @return the response containing updated ID and message
     */
    StationActionResponseDTO updateStation(Long id, StationRequestDTO requestDTO);

    /**
     * Update the status of a station and optionally cascade updates to stationZones.
     *
     * @param id         the ID of the station
     * @param requestDTO the new status wrapped in a DTO
     * @return the response containing station ID and message
     */
    StationActionResponseDTO updateStationStatus(Long id, StationStatusRequestDTO requestDTO);

    /**
     * Delete an existing station.
     * Checks if there are any active ride sessions before deletion.
     *
     * @param id the ID of the station to delete
     * @return the response containing deleted station ID and message
     */
    StationActionResponseDTO deleteStation(Long id);
}