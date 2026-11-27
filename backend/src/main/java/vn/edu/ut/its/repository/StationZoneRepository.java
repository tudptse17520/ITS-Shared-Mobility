package vn.edu.ut.its.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ut.its.entity.StationZone;
import java.util.List; // Duy Kha đã thêm thư viện này

/**
 * Repository interface for StationZone entity.
 */
@Repository
public interface StationZoneRepository extends JpaRepository<StationZone, Long> {
    // Tối ưu việc tìm kiếm khu trạm theo trạm xe (Station ID)
    List<StationZone> findByStationId(Long stationId);
}
