package vn.edu.ut.its.dto.request;

import jakarta.validation.constraints.NotNull;
import vn.edu.ut.its.constant.PaymentStatus;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Quản trị viên có thể chuyển giao dịch đang chờ sang SUCCESS hoặc FAILED.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualStatusRequestDTO {
    @NotNull(message = "Trạng thái mới không được để trống.")
    private PaymentStatus status;
    @NotBlank(message = "Lý do cập nhật thủ công không được để trống.")
    private String note;
}
