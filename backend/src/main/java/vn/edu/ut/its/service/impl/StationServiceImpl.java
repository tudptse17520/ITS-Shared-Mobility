package vn.edu.ut.its.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ut.its.constant.StationStatus;
import vn.edu.ut.its.constant.StationZoneStatus;
import vn.edu.ut.its.constant.RideSessionStatus;
import vn.edu.ut.its.constant.StationDockStatus;
import vn.edu.ut.its.dto.request.StationRequestDTO;
import vn.edu.ut.its.dto.request.StationStatusRequestDTO;
import vn.edu.ut.its.dto.response.StationDetailResponseDTO;
import vn.edu.ut.its.dto.response.StationListResponseDTO;
import vn.edu.ut.its.dto.response.StationActionResponseDTO;
import vn.edu.ut.its.entity.Station;
import vn.edu.ut.its.entity.StationZone;
import vn.edu.ut.its.entity.StationDock;
import vn.edu.ut.its.exception.ConflictException;
import vn.edu.ut.its.exception.ResourceNotFoundException;
import vn.edu.ut.its.repository.StationRepository;
import vn.edu.ut.its.repository.StationZoneRepository;
import vn.edu.ut.its.repository.StationDockRepository;
import vn.edu.ut.its.service.StationService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of StationService.
 * Handles all core business rules and real-time analytical queries.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StationServiceImpl implements StationService {

    private final StationRepository stationRepository;
    private final StationZoneRepository stationZoneRepository;
    private final StationDockRepository stationDockRepository;
    private final EntityManager entityManager;
    private final ModelMapper modelMapper;

    // ==================== GET - Lấy danh sách (Có lọc & JOIN FETCH) ====================

    @Override
    @Transactional(readOnly = true)
    public StationListResponseDTO getStations(String keyword, StationStatus status) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Station> query = cb.createQuery(Station.class);
        Root<Station> root = query.from(Station.class);

        // Nạp trước danh sách stationZones để tránh lỗi N+1 Query
        root.fetch("stationZones", JoinType.LEFT);
        
        // Tránh trùng lặp do LEFT JOIN FETCH
        query.distinct(true);

        Predicate predicate = cb.conjunction();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            Predicate namePredicate = cb.like(cb.lower(root.get("stationName")), pattern);
            Predicate addressPredicate = cb.like(cb.lower(root.get("address")), pattern);
            predicate = cb.and(predicate, cb.or(namePredicate, addressPredicate));
        }

        if (status != null) {
            predicate = cb.and(predicate, cb.equal(root.get("status"), status));
        }

        query.where(predicate);

        List<Station> stations = entityManager.createQuery(query).getResultList();

        java.util.Map<Long, Long> availableDocksByStation = new java.util.HashMap<>();
        for (Object[] row : stationDockRepository.countDocksGroupedByStation(StationDockStatus.AVAILABLE)) {
            if (row[0] instanceof Long stId && row[1] instanceof Number cnt) {
                availableDocksByStation.put(stId, cnt.longValue());
            }
        }

        List<StationDetailResponseDTO> detailList = stations.stream()
                .map(station -> {
                    StationDetailResponseDTO dto = modelMapper.map(station, StationDetailResponseDTO.class);
                    dto.setTotalAvailableDocks(availableDocksByStation.getOrDefault(station.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());

        return StationListResponseDTO.builder()
                .message("Lấy danh sách trạm xe thành công.")
                .data(detailList)
                .build();
    }

    // ==================== POST - Thêm mới ====================

    @Override
    public StationActionResponseDTO createStation(StationRequestDTO requestDTO) {
        if (stationRepository.existsByStationName(requestDTO.getStationName())) {
            throw new ConflictException(
                    "Trạm xe '" + requestDTO.getStationName() + "' đã tồn tại trong hệ thống.");
        }

        Station station = Station.builder()
                .stationName(requestDTO.getStationName())
                .address(requestDTO.getAddress())
                .numberOfZones(requestDTO.getNumberOfZones())
                .status(requestDTO.getStatus() != null ? requestDTO.getStatus() : StationStatus.ACTIVE)
                .build();

        Station savedStation = stationRepository.save(station);

        return StationActionResponseDTO.builder()
                .id(savedStation.getId())
                .message("Thêm trạm xe thành công.")
                .build();
    }

    // ==================== PUT - Cập nhật ====================

    @Override
    public StationActionResponseDTO updateStation(Long id, StationRequestDTO requestDTO) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trạm xe với ID: " + id));

        boolean isDuplicate = stationRepository.existsByStationNameAndIdNot(requestDTO.getStationName(), id);
        if (isDuplicate) {
            throw new ConflictException("Trạm xe '" + requestDTO.getStationName() + "' đã tồn tại trong hệ thống.");
        }

        station.setStationName(requestDTO.getStationName());
        station.setAddress(requestDTO.getAddress());
        station.setNumberOfZones(requestDTO.getNumberOfZones());
        if (requestDTO.getStatus() != null) {
            station.setStatus(requestDTO.getStatus());
        }

        stationRepository.save(station);

        return StationActionResponseDTO.builder()
                .id(station.getId())
                .message("Cập nhật trạm xe thành công.")
                .build();
    }

    // ==================== PATCH - Cập nhật trạng thái + Cascade Zones & Docks ====================

    @Override
    public StationActionResponseDTO updateStationStatus(Long id, StationStatusRequestDTO requestDTO) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trạm xe với ID: " + id));

        StationStatus newStatus = requestDTO.getStatus();
        station.setStatus(newStatus);
        stationRepository.save(station);

        List<StationZone> stationZones = station.getStationZones();
        if (stationZones != null && !stationZones.isEmpty()) {
            StationZoneStatus targetZoneStatus;
            StationDockStatus targetDockStatus;
            
            switch (newStatus) {
                case MAINTENANCE:
                    targetZoneStatus = StationZoneStatus.MAINTENANCE;
                    targetDockStatus = StationDockStatus.MAINTENANCE;
                    break;
                case INACTIVE:
                    targetZoneStatus = StationZoneStatus.LOCKED;
                    targetDockStatus = StationDockStatus.LOCKED;
                    break;
                case ACTIVE:
                default:
                    targetZoneStatus = StationZoneStatus.ACTIVE;
                    targetDockStatus = StationDockStatus.AVAILABLE;
                    break;
            }

            for (StationZone stationZone : stationZones) {
                stationZone.setStatus(targetZoneStatus);
                List<StationDock> docks = stationZone.getStationDocks();
                if (docks != null) {
                    for (StationDock dock : docks) {
                        if (newStatus == StationStatus.ACTIVE) {
                            // Chỉ kích hoạt lại các dock đang bị khóa hoặc bảo trì
                            if (dock.getStatus() == StationDockStatus.LOCKED || dock.getStatus() == StationDockStatus.MAINTENANCE) {
                                dock.setStatus(StationDockStatus.AVAILABLE);
                            }
                        } else {
                            dock.setStatus(targetDockStatus);
                        }
                    }
                    stationDockRepository.saveAll(docks);
                }
            }
            stationZoneRepository.saveAll(stationZones);
        }

        return StationActionResponseDTO.builder()
                .id(station.getId())
                .message("Cập nhật trạng thái trạm xe thành công.")
                .build();
    }

    // ==================== DELETE - Xóa trạm xe ====================

    @Override
    public StationActionResponseDTO deleteStation(Long id) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trạm xe với ID: " + id));

        // Kiểm tra xem có chuyến đi nào đang diễn ra (IN_PROGRESS và chưa kết thúc) gắn với trạm xe này không
        String jpql = "SELECT COUNT(ps) FROM RideSession ps " +
                      "WHERE ps.stationDock.stationZone.station.id = :stationId " +
                      "AND ps.status = :status AND ps.endedAt IS NULL";
        
        Long count = entityManager.createQuery(jpql, Long.class)
                .setParameter("stationId", id)
                .setParameter("status", RideSessionStatus.IN_PROGRESS)
                .getSingleResult();

        if (count > 0) {
            throw new ConflictException("Không thể xóa trạm xe vì đang có chuyến đi hoạt động tại trạm.");
        }

        List<StationZone> stationZones = station.getStationZones();
        if (stationZones != null && !stationZones.isEmpty()) {
            for (StationZone stationZone : stationZones) {
                if (stationZone.getStationDocks() != null && !stationZone.getStationDocks().isEmpty()) {
                    stationDockRepository.deleteAll(stationZone.getStationDocks());
                }
            }
            stationZoneRepository.deleteAll(stationZones);
        }

        stationRepository.delete(station);

        return StationActionResponseDTO.builder()
                .id(id)
                .message("Xóa trạm xe thành công.")
                .build();
    }
}