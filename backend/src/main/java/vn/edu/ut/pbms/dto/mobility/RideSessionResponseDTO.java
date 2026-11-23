package vn.edu.ut.pbms.dto.mobility;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideSessionResponseDTO {
    private Long sessionId;
    private String sessionCode;
    private Long userId;
    private String username;
    private Long vehicleId;
    private String vehicleCode;
    private String plate;
    private String vehicleBrand;
    private String vehicleTypeName;
    private Double startLatitude;
    private Double startLongitude;
    private Double currentLatitude;
    private Double currentLongitude;
    private Double endLatitude;
    private Double endLongitude;
    private Double distanceKm;
    private Integer currentBatteryLevel;
    private String lockStatus;
    private String startTime;
    private String endTime;
    private Long durationMinutes;
    private BigDecimal estimatedFee;
    private BigDecimal totalFee;
    private String status;
    private Boolean paid;
}

