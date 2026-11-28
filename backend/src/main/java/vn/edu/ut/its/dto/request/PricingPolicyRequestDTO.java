package vn.edu.ut.its.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

import vn.edu.ut.its.constant.PricingPolicyStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PricingPolicyRequestDTO {
    @NotNull(message = "Danh mục loại xe không được để trống.")
    private Long vehicleTypeId;
    @NotNull(message = "Giá cơ bản không được để trống.")
    @Positive(message = "Giá cơ bản phải là số dương.")
    private BigDecimal basePrice;
    @NotNull(message = "Phí phụ thu mỗi giờ không được để trống.")
    @Positive(message = "Phí phụ thu mỗi giờ phải là số dương.")
    private BigDecimal extraFeePerHour;
    @NotNull(message = "Ngày áp dụng không được để trống.")
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate effectiveDate;
    private PricingPolicyStatus status;
}
