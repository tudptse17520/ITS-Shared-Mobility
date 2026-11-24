package vn.edu.ut.its.dto.report;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardReportDTO {
    private int totalRidesStarted;
    private int totalRidesEnded;
    private int activeRides;
    private BigDecimal totalRevenue;
    private Double occupancyRate;
    private String peakHour;

    private Object details;
}
