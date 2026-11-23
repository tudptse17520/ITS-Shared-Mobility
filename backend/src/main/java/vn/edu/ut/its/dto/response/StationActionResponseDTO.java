package vn.edu.ut.its.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for sending station operation result back to the client in API responses.
 * Contains the station ID and a status message.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationActionResponseDTO {

    private Long id;

    private String message;
}
