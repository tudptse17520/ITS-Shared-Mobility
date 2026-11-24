package vn.edu.ut.its.dto.response;

import vn.edu.ut.its.constant.BookingStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingListResponseDTO {
    private Long bookingId;
    private Long stationDockId;
    private String stationDockName;
    private String plannedStartAt;
    private String plannedEndAt;
    private String customerName;
    private String customerPhone;
    private String licensePlate;
    private String vehicleTypeName;

    private BookingStatus status;
}
