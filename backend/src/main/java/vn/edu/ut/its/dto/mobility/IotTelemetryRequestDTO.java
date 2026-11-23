package vn.edu.ut.its.dto.mobility;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IotTelemetryRequestDTO {
    @NotNull(message = "vehicleId không được để trống")
    private Long vehicleId;

    @NotNull(message = "latitude không được để trống")
    private Double latitude;

    @NotNull(message = "longitude không được để trống")
    private Double longitude;

    private Integer batteryLevel;
    private Double speedKmh;
    private String lockStatus;
}

