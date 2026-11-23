package vn.edu.ut.its.dto.mobility;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FleetVehicleResponseDTO {
    private Long id;
    private String vehicleCode;
    private String plate;
    private String brand;
    private String color;
    private Long vehicleTypeId;
    private String vehicleTypeName;
    private Long currentStationId;
    private String currentStationName;
    private Double latitude;
    private Double longitude;
    private Integer batteryLevel;
    private String lockStatus;
    private String fleetStatus;
    private Double distanceMeters;
    private BigDecimal unlockFee;
    private BigDecimal hourlyRate;
}

