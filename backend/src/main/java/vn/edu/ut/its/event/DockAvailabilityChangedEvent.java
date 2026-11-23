package vn.edu.ut.its.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DockAvailabilityChangedEvent extends ApplicationEvent {
    private final Long stationId;
    private final Long stationZoneId;

    public DockAvailabilityChangedEvent(Object source, Long stationId, Long stationZoneId) {
        super(source);
        this.stationId = stationId;
        this.stationZoneId = stationZoneId;
    }
}
