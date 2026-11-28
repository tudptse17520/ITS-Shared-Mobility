package vn.edu.ut.its.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ut.its.entity.StationZone;

@Repository
public interface StationZoneRepository extends JpaRepository<StationZone, Long> {
}
