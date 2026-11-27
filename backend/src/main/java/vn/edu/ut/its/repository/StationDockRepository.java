package vn.edu.ut.its.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ut.its.constant.StationDockStatus;
import vn.edu.ut.its.entity.StationDock;

@Repository
public interface StationDockRepository extends JpaRepository<StationDock, Long> {
    long countByStatus(StationDockStatus status);
    
    // Tìm kiếm danh sách vị trí dock theo mã khu trạm (StationZone ID)
    List<StationDock> findByStationZoneId(Long stationZoneId);
    
    // Tìm kiếm danh sách vị trí dock theo trạng thái (AVAILABLE, OCCUPIED,...)
    List<StationDock> findByStatus(StationDockStatus status);
    List<StationDock> findByStationZoneIdAndStatus(Long stationZoneId, StationDockStatus status);
    List<StationDock> findByStationZone_Id(Long stationZoneId);
    boolean existsByStationZoneIdAndDockName(Long stationZoneId, String dockName);
    
    // Đếm số lượng vị trí dock theo khu trạm và trạng thái
    long countByStationZoneIdAndStatus(Long stationZoneId, StationDockStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT d.stationZone.station.id, COUNT(d) FROM StationDock d WHERE d.status = :status GROUP BY d.stationZone.station.id")
    List<Object[]> countDocksGroupedByStation(@org.springframework.data.repository.query.Param("status") StationDockStatus status);
}
