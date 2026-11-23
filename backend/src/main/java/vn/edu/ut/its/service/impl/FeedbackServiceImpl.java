package vn.edu.ut.its.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.ut.its.constant.FeedbackStatus;
import vn.edu.ut.its.constant.IssueType;
import vn.edu.ut.its.dto.request.FeedbackRequest;
import vn.edu.ut.its.dto.response.FeedbackResponse;
import vn.edu.ut.its.entity.Feedback;
import vn.edu.ut.its.entity.RideSession;
import vn.edu.ut.its.exception.ResourceNotFoundException;
import vn.edu.ut.its.repository.FeedbackRepository;
import vn.edu.ut.its.repository.RideSessionRepository;
import vn.edu.ut.its.service.FeedbackService;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final RideSessionRepository rideSessionRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getAllFeedbacks(IssueType issueType, FeedbackStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Feedback> feedbacks = feedbackRepository.findAllByFilters(issueType, status, pageable);
        return feedbacks.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackResponse getFeedbackById(Long id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found with ID: " + id));
        return mapToResponse(feedback);
    }

    @Override
    @Transactional
    public FeedbackResponse createFeedback(FeedbackRequest request) {
        rejectLegacyIssueType(request.getIssueType());
        RideSession session = rideSessionRepository.findById(request.getRideSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Ride Session not found with ID: " + request.getRideSessionId()));

        Feedback feedback = Feedback.builder()
                .rideSession(session)
                .issueType(request.getIssueType())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : FeedbackStatus.REPORTED)
                .build();

        Feedback savedFeedback = feedbackRepository.save(feedback);
        return mapToResponse(savedFeedback);
    }

    @Override
    @Transactional
    public FeedbackResponse updateFeedback(Long id, FeedbackRequest request) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found with ID: " + id));
        if (feedback.getIssueType() != request.getIssueType()) {
            rejectLegacyIssueType(request.getIssueType());
        }

        RideSession session = rideSessionRepository.findById(request.getRideSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Ride Session not found with ID: " + request.getRideSessionId()));

        feedback.setRideSession(session);
        feedback.setIssueType(request.getIssueType());
        feedback.setDescription(request.getDescription());
        
        if (request.getStatus() != null) {
            feedback.setStatus(request.getStatus());
        }

        Feedback updatedFeedback = feedbackRepository.save(feedback);
        return mapToResponse(updatedFeedback);
    }

    private void rejectLegacyIssueType(IssueType issueType) {
        if (issueType == IssueType.LOST_TICKET) {
            throw new vn.edu.ut.its.exception.BusinessRuleViolationException("Vui lòng chọn loại sự cố của chuyến đi.");
        }
    }

    @Override
    @Transactional
    public void deleteFeedback(Long id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found with ID: " + id));
        feedbackRepository.delete(feedback);
    }

    private FeedbackResponse mapToResponse(Feedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .rideSessionId(feedback.getRideSession() != null ? feedback.getRideSession().getId() : null)
                .issueType(feedback.getIssueType())
                .description(feedback.getDescription())
                .status(feedback.getStatus())
                .build();
    }
}
