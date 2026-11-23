package vn.edu.ut.its.dto.response;

import lombok.Builder;
import lombok.Data;
import vn.edu.ut.its.constant.FeedbackStatus;
import vn.edu.ut.its.constant.IssueType;

@Data
@Builder
public class FeedbackResponse {
    private Long id;
    private Long rideSessionId;
    private IssueType issueType;
    private String description;
    private FeedbackStatus status;
}
