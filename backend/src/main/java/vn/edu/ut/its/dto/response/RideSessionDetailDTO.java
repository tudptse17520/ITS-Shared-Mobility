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
    private String sessionCode;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private BigDecimal totalFee;
    private Long vehicleId;
    private Long stationDockId;
}
