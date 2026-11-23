package vn.edu.ut.pbms.dto.mobility;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IotControlRequestDTO {
    /**
     * Supported commands: UNLOCK, LOCK, RECHARGE_100, SET_BATTERY, MAINTENANCE, SET_AVAILABLE
     */
    @NotBlank(message = "Lệnh điều khiển không được để trống")
    private String command;

    private Integer batteryLevel;
    private Double latitude;
    private Double longitude;
}

