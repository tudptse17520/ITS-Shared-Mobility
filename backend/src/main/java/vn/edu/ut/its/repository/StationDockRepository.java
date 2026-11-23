package vn.edu.ut.its.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ut.its.constant.StationDockStatus;
import vn.edu.ut.its.entity.StationDock;

@Repository
public interface StationDockRepository extends JpaRepository<StationDock, Long> {
    long countByStatus(StationDockStatus status);
    
    // Tìm kiếm danh sách ô đỗ xe theo mã tầng (StationZone ID)
    List<StationDock> findByStationZoneId(Long stationZoneId);
    
    // Tìm kiếm danh sách ô đỗ xe theo trạng thái (AVAILABLE, OCCUPIED,...)
    List<StationDock> findByStatus(StationDockStatus status);
    List<StationDock> findByStationZoneIdAndStatus(Long stationZoneId, StationDockStatus status);
    List<StationDock> findByStationZone_Id(Long stationZoneId);
    boolean existsByStationZoneIdAndDockName(Long stationZoneId, String dockName);
    
    // Đếm số lượng ô đỗ xe theo tầng và trạng thái
    long countByStationZoneIdAndStatus(Long stationZoneId, StationDockStatus status);
}
