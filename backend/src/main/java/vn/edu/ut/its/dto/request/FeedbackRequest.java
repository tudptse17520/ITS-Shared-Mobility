package vn.edu.ut.its.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import vn.edu.ut.its.constant.FeedbackStatus;
import vn.edu.ut.its.constant.IssueType;

@Data
public class FeedbackRequest {
    @NotNull(message = "Ride Session ID is required")
    private Long rideSessionId;

    @NotNull(message = "Issue Type is required")
    private IssueType issueType;

    @NotBlank(message = "Description cannot be blank")
    private String description;

    @NotNull(message = "Status is required")
    private FeedbackStatus status;
}
