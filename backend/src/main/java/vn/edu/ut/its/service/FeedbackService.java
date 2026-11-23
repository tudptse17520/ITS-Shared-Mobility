package vn.edu.ut.its.service;

import org.springframework.data.domain.Page;
import vn.edu.ut.its.constant.FeedbackStatus;
import vn.edu.ut.its.constant.IssueType;
import vn.edu.ut.its.dto.request.FeedbackRequest;
import vn.edu.ut.its.dto.response.FeedbackResponse;

public interface FeedbackService {
    Page<FeedbackResponse> getAllFeedbacks(IssueType issueType, FeedbackStatus status, int page, int size);
    FeedbackResponse getFeedbackById(Long id);
    FeedbackResponse createFeedback(FeedbackRequest request);
    FeedbackResponse updateFeedback(Long id, FeedbackRequest request);
    void deleteFeedback(Long id);
}
