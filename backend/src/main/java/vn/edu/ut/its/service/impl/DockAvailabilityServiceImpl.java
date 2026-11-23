package vn.edu.ut.its.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ut.its.constant.StationDockStatus;
import vn.edu.ut.its.entity.StationDock;
import vn.edu.ut.its.event.DockAvailabilityChangedEvent;
import vn.edu.ut.its.exception.BusinessRuleViolationException;
import vn.edu.ut.its.service.DockAvailabilityService;

@Service
@RequiredArgsConstructor
public class DockAvailabilityServiceImpl implements DockAvailabilityService {

    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void updateDockStatus(Long dockId, StationDockStatus newStatus) {
        // Use PESSIMISTIC_WRITE lock to prevent concurrent modifications
        StationDock dock = entityManager.find(StationDock.class, dockId, LockModeType.PESSIMISTIC_WRITE);
        
        if (dock == null) {
            throw new BusinessRuleViolationException("Station dock not found with id: " + dockId);
        }

        // Only update if status is actually changing
        if (dock.getStatus() != newStatus) {
            dock.setStatus(newStatus);
            
            // Force flush so that listeners see the updated state if they run in a new transaction
            entityManager.flush();
            
            // Publish event. Listener will handle the actual broadcast after commit.
            eventPublisher.publishEvent(new DockAvailabilityChangedEvent(
                    this, 
                    dock.getStationZone().getStation().getId(), 
                    dock.getStationZone().getId()
            ));
        }
    }
}
