package vn.edu.ut.its.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import vn.edu.ut.its.constant.BookingStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDTO {
    private Long id;
    private BookingStatus status;
    private String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long expiresAtEpochMillis;
}
