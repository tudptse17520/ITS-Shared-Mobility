package vn.edu.ut.its.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import vn.edu.ut.its.constant.StationStatus;
import vn.edu.ut.its.dto.request.StationRequestDTO;
import vn.edu.ut.its.dto.request.StationStatusRequestDTO;
import vn.edu.ut.its.dto.response.StationListResponseDTO;
import vn.edu.ut.its.dto.response.StationActionResponseDTO;
import vn.edu.ut.its.service.StationService;
import vn.edu.ut.its.service.SseEmitterManager;

import java.util.List;

/**
 * REST Controller for Station Management.
 * Base endpoint: /api/v1/stations
 */
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/stations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "Station")
public class StationController {

    private final StationService stationService;
    private final SseEmitterManager sseEmitterManager;

    @GetMapping("/{id}/availability-stream")
    @PreAuthorize("permitAll()")
    public SseEmitter subscribeToAvailability(@PathVariable Long id) {
        return sseEmitterManager.subscribe(id);
    }

    @GetMapping("/availability-stream")
    @PreAuthorize("permitAll()")
    public SseEmitter subscribeToGlobalAvailability() {
        return sseEmitterManager.subscribe(null); // Subscribe to global
    }

    // ==================== GET - Tra cứu ====================

    /**
     * Retrieve list of stations filtered by status.
     *
     * @param status the status of the station
     * @return HTTP 200 with the list of stations
     */
    @GetMapping
    public ResponseEntity<StationListResponseDTO> getStations(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) StationStatus status) {
        
        StationListResponseDTO response = stationService.getStations(keyword, status);
        return ResponseEntity.ok(response);
    }

    // ==================== POST - Thêm trạm xe mới ====================

    /**
     * Create a new station.
     *
     * @param requestDTO the station data (validated)
     * @return HTTP 201 with the created station ID and message
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StationActionResponseDTO> createStation(
            @Valid @RequestBody StationRequestDTO requestDTO) {
        StationActionResponseDTO createdStation = stationService.createStation(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdStation);
    }

    // ==================== PUT - Cập nhật thông tin ====================

    /**
     * Update an existing station's details.
     *
     * @param id         the station ID from path
     * @param requestDTO the updated station data (validated)
     * @return HTTP 200 with the updated station ID and success message
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StationActionResponseDTO> updateStation(
            @PathVariable Long id,
            @Valid @RequestBody StationRequestDTO requestDTO) {
        StationActionResponseDTO updatedStation = stationService.updateStation(id, requestDTO);
        return ResponseEntity.ok(updatedStation);
    }

    // ==================== PATCH - Cập nhật trạng thái ====================

    /**
     * Update a station's status and cascade status to its stationZones.
     *
     * @param id         the station ID from path
     * @param requestDTO the status data (validated)
     * @return HTTP 200 with station ID and success message
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StationActionResponseDTO> updateStationStatus(
            @PathVariable Long id,
            @Valid @RequestBody StationStatusRequestDTO requestDTO) {
        StationActionResponseDTO updatedStation = stationService.updateStationStatus(id, requestDTO);
        return ResponseEntity.ok(updatedStation);
    }

    // ==================== DELETE - Xóa trạm xe ====================

    /**
     * Delete an existing station.
     * * @param id the station ID from path
     * 
     * @return HTTP 200 with station ID and success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StationActionResponseDTO> deleteStation(@PathVariable Long id) {
        StationActionResponseDTO deletedStation = stationService.deleteStation(id);
        return ResponseEntity.ok(deletedStation);
    }
}