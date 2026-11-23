package vn.edu.ut.its.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ut.its.entity.Station;

/**
 * Repository interface for Station entity.
 * Provides basic CRUD operations and custom query methods if needed.
 */
@Repository
public interface StationRepository extends JpaRepository<Station, Long> {

    boolean existsByStationName(String stationName);

    boolean existsByStationNameAndIdNot(String stationName, Long id);
}

