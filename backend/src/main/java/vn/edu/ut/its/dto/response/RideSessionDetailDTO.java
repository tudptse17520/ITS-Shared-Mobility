package vn.edu.ut.its.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideSessionDetailDTO {
    
    private Long id;
    
    private String plate;
    private String vehicleCode;
    private String vehicleTypeName;
    private String sessionCode;
    private String customerName;
    private String customerPhone;
    private Double distanceKm;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private BigDecimal totalFee;
    private Long vehicleId;
    private Long stationDockId;
}
