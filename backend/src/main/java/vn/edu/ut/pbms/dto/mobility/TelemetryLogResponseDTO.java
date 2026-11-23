package vn.edu.ut.pbms.dto.mobility;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryLogResponseDTO {
    private Long id;
    private Long vehicleId;
    private String vehicleCode;
    private Long sessionId;
    private Double latitude;
    private Double longitude;
    private Integer batteryLevel;
    private Double speedKmh;
    private String lockStatus;
    private String recordedAt;
}

