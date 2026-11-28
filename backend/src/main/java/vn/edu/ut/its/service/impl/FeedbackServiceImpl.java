package vn.edu.ut.its.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import vn.edu.ut.its.repository.UserRepository;
import vn.edu.ut.its.security.SecurityUtils;
import vn.edu.ut.its.service.FeedbackService;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final RideSessionRepository rideSessionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getAllFeedbacks(IssueType issueType, FeedbackStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Feedback> feedbacks = feedbackRepository.findAllByFilters(issueType, status, pageable);
        return feedbacks.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackResponse getFeedbackById(Long id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found with ID: " + id));
        Long ownerId = feedback.getRideSession() != null && feedback.getRideSession().getUser() != null
                ? feedback.getRideSession().getUser().getId()
                : null;
        SecurityUtils.assertAdminOrOwner(ownerId, userRepository, "Bạn không có quyền xem phản ánh của người dùng khác.");
        return mapToResponse(feedback);
    }

    @Override
    @Transactional
    public FeedbackResponse createFeedback(FeedbackRequest request) {
        RideSession session = rideSessionRepository.findById(request.getRideSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Ride Session not found with ID: " + request.getRideSessionId()));

        Long ownerId = session.getUser() != null ? session.getUser().getId() : null;
        SecurityUtils.assertAdminOrOwner(ownerId, userRepository, "Bạn chỉ có thể gửi phản ánh cho chuyến đi của chính mình.");
        boolean isAdminCaller = SecurityUtils.getAuthenticatedUser(userRepository)
                .map(SecurityUtils::isAdmin)
                .orElse(true);

        Feedback feedback = Feedback.builder()
                .rideSession(session)
                .issueType(request.getIssueType())
                .description(request.getDescription())
                .status(isAdminCaller && request.getStatus() != null ? request.getStatus() : FeedbackStatus.REPORTED)
                .build();

        Feedback savedFeedback = feedbackRepository.save(feedback);
        return mapToResponse(savedFeedback);
    }

    @Override
    @Transactional
    public FeedbackResponse updateFeedback(Long id, FeedbackRequest request) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found with ID: " + id));

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


    @Override
    @Transactional
    public void deleteFeedback(Long id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found with ID: " + id));
        feedbackRepository.delete(feedback);
    }

    private FeedbackResponse mapToResponse(Feedback feedback) {
        RideSession session = feedback.getRideSession();
        String customerName = null;
        if (session != null && session.getUser() != null) {
            String fullName = session.getUser().getFullName();
            customerName = (fullName != null && !fullName.isBlank()) ? fullName : session.getUser().getUsername();
        }
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .rideSessionId(session != null ? session.getId() : null)
                .sessionCode(session != null ? session.getSessionCode() : null)
                .vehicleCode(session != null && session.getVehicle() != null ? session.getVehicle().getVehicleCode() : null)
                .licensePlate(session != null ? session.getPlate() : null)
                .customerName(customerName)
                .issueType(feedback.getIssueType())
                .description(feedback.getDescription())
                .status(feedback.getStatus())
                .build();
    }
}
