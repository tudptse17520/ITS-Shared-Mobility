package vn.edu.ut.its.dto.mobility;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationResponseDTO {
    private Long id;
    private String stationName;
    private String address;
    private Double latitude;
    private Double longitude;
    private Integer totalDocks;
    private Integer availableVehicles;
    private String status;
}

