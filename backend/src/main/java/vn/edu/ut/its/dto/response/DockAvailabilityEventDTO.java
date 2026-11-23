package vn.edu.ut.its.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DockAvailabilityEventDTO {
    private Long stationId;
    private Long stationZoneId;
    private Long availableDocks;
    private Long stationAvailableDocks;
}
