package vn.edu.ut.its.dto.report;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideFlowReportDTO {
    private int totalRidesStarted;
    private int totalRidesEnded;

    private List<?> details;
}