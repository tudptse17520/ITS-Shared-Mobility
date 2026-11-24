package vn.edu.ut.its.dto.request;

import jakarta.validation.constraints.NotNull;
import vn.edu.ut.its.constant.UserStatus;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating user status.
 * Uses the UserStatus enum for the status field.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {

    @NotNull(message = "Trạng thái không được để trống.")
    private UserStatus status;
}
