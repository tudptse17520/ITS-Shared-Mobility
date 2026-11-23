package vn.edu.ut.its.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.ut.its.constant.StationStatus;

/**
 * DTO for updating station status only (PATCH).
 * Contains a single status field mapped to StationStatus enum.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StationStatusRequestDTO {
    @NotNull(message = "Trạng thái không được để trống.")
    private StationStatus status;
}
