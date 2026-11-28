package vn.edu.ut.its.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

import vn.edu.ut.its.constant.PricingPolicyStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingPolicyResponseDTO {

    private Long id;
    private Long vehicleTypeId;
    private BigDecimal basePrice;
    private BigDecimal extraFeePerHour;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate effectiveDate;
    private PricingPolicyStatus status;
}
