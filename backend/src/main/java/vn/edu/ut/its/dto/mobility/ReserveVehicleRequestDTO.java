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
public class ReserveVehicleRequestDTO {
    @NotNull(message = "vehicleId không được để trống")
    private Long vehicleId;

    private Long userId;

    private Integer holdMinutes;
}

