package vn.edu.ut.its.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ut.its.constant.StationDockStatus;
import vn.edu.ut.its.entity.StationDock;

import java.util.List;

@Repository
public interface StationDockRepository extends JpaRepository<StationDock, Long> {
    @Query("SELECT d.stationZone.station.id, COUNT(d) FROM StationDock d WHERE d.status = :status GROUP BY d.stationZone.station.id")
    List<Object[]> countDocksGroupedByStation(@Param("status") StationDockStatus status);
}
