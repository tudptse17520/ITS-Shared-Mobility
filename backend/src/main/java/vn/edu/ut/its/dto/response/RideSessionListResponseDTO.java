package vn.edu.ut.its.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideSessionListResponseDTO {
    private int totalItems;

    private List<RideSessionDetailDTO> data;
}
