package vn.edu.ut.its.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.ut.its.constant.StationStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationDetailResponseDTO {
    private Long id;
    private String stationName;
    private String address;
    private Integer numberOfZones;
    private StationStatus status;
    private Long totalAvailableDocks;
}