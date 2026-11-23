package vn.edu.ut.its.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for sending station list data back to the client in API responses.
 * Contains a message and a list of station details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationListResponseDTO {

    private String message;

    private List<StationDetailResponseDTO> data;
}
