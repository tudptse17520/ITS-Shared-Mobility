package vn.edu.ut.its.dto.mobility;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndRideRequestDTO {
    private Double endLatitude;
    private Double endLongitude;
    private Long endStationId;
}

