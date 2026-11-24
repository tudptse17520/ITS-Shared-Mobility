package vn.edu.ut.its.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestDTO {


    @NotNull(message = "Mã phương tiện không được để trống")
    private Long vehicleId;

    @NotNull(message = "Mã vị trí đỗ không được để trống")
    private Long stationDockId;

    @NotBlank(message = "Thời điểm bắt đầu dự kiến không được để trống")
    private String plannedStartAt;

    @NotBlank(message = "Thời điểm kết thúc dự kiến không được để trống")
    private String plannedEndAt;
}
