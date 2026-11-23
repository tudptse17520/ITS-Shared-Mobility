package vn.edu.ut.its.dto.mobility;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartRideRequestDTO {
    private Long vehicleId;
    private String vehicleCode;
    private Long userId;
    private Double latitude;
    private Double longitude;
}

