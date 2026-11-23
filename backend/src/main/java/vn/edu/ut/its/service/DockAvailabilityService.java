package vn.edu.ut.its.service;

import vn.edu.ut.its.constant.StationDockStatus;

public interface DockAvailabilityService {
    
    /**
     * Updates the status of a station dock with pessimistic locking to prevent race conditions.
     * Fires an event to update SSE clients after the transaction commits.
     * 
     * @param dockId the ID of the station dock
     * @param newStatus the new status
     */
    void updateDockStatus(Long dockId, StationDockStatus newStatus);
}
