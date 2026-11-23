package vn.edu.ut.its.dto.request;

import vn.edu.ut.its.constant.Role;
import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing user.
 * Username and password are not modifiable through this endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    @NotBlank(message = "Họ tên không được để trống.")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống.")
    @Pattern(regexp = "^(0[35789])+([0-9]{8})$", message = "Số điện thoại không đúng định dạng.")
    private String phoneNumber;

    @NotNull(message = "Vai trò không được để trống.")
    private Role role;
}
