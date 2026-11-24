package vn.edu.ut.its.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.ut.its.constant.StationStatus;

/**
 * DTO for receiving station data from client requests (Create / Update).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StationRequestDTO {
    @NotBlank(message = "Tên trạm xe không được để trống.")
    @Size(max = 100, message = "Tên trạm xe không được vượt quá 100 ký tự.")
    private String stationName;
    @NotBlank(message = "Địa chỉ không được để trống.")
    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự.")
    private String address;
    @NotNull(message = "Số khu không được để trống.")
    @Min(value = 1, message = "Số khu phải lớn hơn hoặc bằng 1.")
    private Integer numberOfZones;
    private StationStatus status;
}
