package vn.edu.ut.its.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.ut.its.constant.FeedbackStatus;
import vn.edu.ut.its.constant.IssueType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {
    private Long id;
    private Long rideSessionId;
    private String sessionCode;
    private String vehicleCode;
    private String licensePlate;
    private String customerName;
    private IssueType issueType;
    private String description;
    private FeedbackStatus status;
}
