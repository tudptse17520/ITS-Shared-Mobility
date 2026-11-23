package vn.edu.ut.its.event;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.edu.ut.its.dto.response.DockAvailabilityEventDTO;
import vn.edu.ut.its.service.SseEmitterManager;

@Component
@RequiredArgsConstructor
public class DockAvailabilityEventListener {

    private final EntityManager entityManager;
    private final SseEmitterManager sseEmitterManager;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void handleDockAvailabilityChanged(DockAvailabilityChangedEvent event) {
        Long stationId = event.getStationId();
        Long stationZoneId = event.getStationZoneId();

        // Query the new available count for this stationZone
        String countHql = "SELECT COUNT(p) FROM StationDock p WHERE p.stationZone.id = :stationZoneId AND p.status = 'AVAILABLE'";
        Long availableDocks = entityManager.createQuery(countHql, Long.class)
                .setParameter("stationZoneId", stationZoneId)
                .getSingleResult();

        // Query the new available count for the entire station
        String stationCountHql = "SELECT COUNT(p) FROM StationDock p WHERE p.stationZone.station.id = :stationId AND p.status = 'AVAILABLE'";
        Long stationAvailableDocks = entityManager.createQuery(stationCountHql, Long.class)
                .setParameter("stationId", stationId)
                .getSingleResult();

        // Create payload
        DockAvailabilityEventDTO payload = DockAvailabilityEventDTO.builder()
                .stationId(stationId)
                .stationZoneId(stationZoneId)
                .availableDocks(availableDocks)
                .stationAvailableDocks(stationAvailableDocks)
                .build();

        // Broadcast to clients subscribed to this station
        sseEmitterManager.broadcastToStation(stationId, payload);
    }
}
