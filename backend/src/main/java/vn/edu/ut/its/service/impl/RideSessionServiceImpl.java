package vn.edu.ut.its.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ut.its.constant.StationDockStatus;
import vn.edu.ut.its.constant.RideSessionStatus;
import vn.edu.ut.its.dto.response.RideSessionListResponseDTO;
import vn.edu.ut.its.dto.response.RideSessionDetailDTO;
import vn.edu.ut.its.entity.RideSession;
import vn.edu.ut.its.exception.BusinessRuleViolationException;
import vn.edu.ut.its.exception.ResourceNotFoundException;
import vn.edu.ut.its.repository.FeedbackRepository;
import vn.edu.ut.its.repository.RideSessionRepository;
import vn.edu.ut.its.repository.PaymentRepository;
import vn.edu.ut.its.service.RideSessionService;
import vn.edu.ut.its.service.DockAvailabilityService;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of RideSessionService.
 * Xử lý tra cứu lịch sử lượt thuê / gửi phương tiện.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RideSessionServiceImpl implements RideSessionService {

    private final EntityManager entityManager;
    private final ModelMapper modelMapper;
    private final RideSessionRepository rideSessionRepository;
    private final DockAvailabilityService dockAvailabilityService;
    private final FeedbackRepository feedbackRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Tra cứu danh sách lượt gửi xe với bộ lọc động (Dynamic Query) dùng Criteria API.
     */
    @Override
    @Transactional(readOnly = true)
    public RideSessionListResponseDTO getRideSessions(String plate, String status, String fromDate) {

        // ==================== Bước 1: Validate tham số đầu vào ====================

        // Kiểm tra tính hợp lệ của status: phải thuộc đúng Enum (IN_PROGRESS, COMPLETED)
        RideSessionStatus parsedStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                parsedStatus = RideSessionStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessRuleViolationException(
                        "Trạng thái '" + status + "' không hợp lệ. Chỉ chấp nhận: IN_PROGRESS, COMPLETED.");
            }
        }

        // Ép chuỗi from_date sang LocalDateTime, kiểm tra định dạng hợp lệ
        LocalDateTime parsedFromDate = null;
        if (fromDate != null && !fromDate.trim().isEmpty()) {
            String trimmed = fromDate.trim();
            try {
                parsedFromDate = LocalDateTime.parse(trimmed);
            } catch (DateTimeParseException e1) {
                try {
                    // Fallback 1: chuỗi chỉ có ngày (VD: 2026-07-12)
                    parsedFromDate = java.time.LocalDate.parse(trimmed).atStartOfDay();
                } catch (DateTimeParseException e2) {
                    try {
                        // Fallback 2: chuỗi có múi giờ
                        parsedFromDate = ZonedDateTime.parse(trimmed).toLocalDateTime();
                    } catch (DateTimeParseException e3) {
                        throw new BusinessRuleViolationException(
                                "Định dạng ngày '" + fromDate + "' không hợp lệ. Vui lòng dùng định dạng ISO-8601 (yyyy-MM-ddTHH:mm:ss hoặc yyyy-MM-dd)");
                    }
                }
            }
        }

        // ==================== Bước 2: Xây dựng truy vấn động (Dynamic Query) ====================

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<RideSession> query = cb.createQuery(RideSession.class);
        Root<RideSession> root = query.from(RideSession.class);

        List<Predicate> predicates = new ArrayList<>();

        // Bộ lọc 1: Biển số xe (plate) - Tìm kiếm chính xác, không phân biệt hoa/thường
        if (plate != null && !plate.trim().isEmpty()) {
            predicates.add(cb.equal(cb.upper(root.get("plate")), plate.trim().toUpperCase()));
        }

        // Bộ lọc 2: Trạng thái (status)
        if (parsedStatus != null) {
            predicates.add(cb.equal(root.get("status"), parsedStatus));
        }

        // Bộ lọc 3: Từ ngày (from_date) - Lọc mốc thời gian vào bãi (time_in >= from_date)
        if (parsedFromDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("timeIn"), parsedFromDate));
        }

        if (!predicates.isEmpty()) {
            query.where(predicates.toArray(new Predicate[0]));
        }

        // Sắp xếp mặc định theo thời gian vào (mới nhất lên trước)
        query.orderBy(cb.desc(root.get("timeIn")));

        // ==================== Bước 3: Truy xuất cơ sở dữ liệu (Database Fetch) ====================

        List<RideSession> sessions = entityManager.createQuery(query).getResultList();

        // ==================== Bước 4: Đóng gói và Phản hồi (Flattening & Response) ====================

        List<RideSessionDetailDTO> data = sessions.stream()
                .map(session -> modelMapper.map(session, RideSessionDetailDTO.class))
                .collect(Collectors.toList());

        return RideSessionListResponseDTO.builder()
                .totalItems(data.size())
                .data(data)
                .build();
    }

    /**
     * Tra cứu danh sách lượt gửi xe của một user cụ thể.
     */
    @Override
    @Transactional(readOnly = true)
    public RideSessionListResponseDTO getRideSessionsByUserId(Long userId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<RideSession> query = cb.createQuery(RideSession.class);
        Root<RideSession> root = query.from(RideSession.class);

        query.where(cb.equal(root.get("user").get("id"), userId));
        query.orderBy(cb.desc(root.get("timeIn")));

        List<RideSession> sessions = entityManager.createQuery(query).getResultList();

        List<RideSessionDetailDTO> data = sessions.stream()
                .map(session -> modelMapper.map(session, RideSessionDetailDTO.class))
                .collect(Collectors.toList());

        return RideSessionListResponseDTO.builder()
                .totalItems(data.size())
                .data(data)
                .build();
    }

    @Override
    public RideSessionDetailDTO updateSessionStatus(Long id, String status) {
        RideSession session = rideSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt gửi xe với ID: " + id));

        if (status == null || status.isBlank()) {
            throw new BusinessRuleViolationException("Trạng thái lượt gửi xe không được để trống.");
        }

        RideSessionStatus newStatus;
        try {
            newStatus = RideSessionStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleViolationException("Trạng thái '" + status + "' không hợp lệ.");
        }

        session.setStatus(newStatus);
        if (newStatus == RideSessionStatus.COMPLETED) {
            if (session.getTimeOut() == null) {
                session.setTimeOut(LocalDateTime.now());
            }
            if (session.getStationDock() != null) {
                dockAvailabilityService.updateDockStatus(session.getStationDock().getId(), StationDockStatus.AVAILABLE);
            }
        }

        RideSession saved = rideSessionRepository.save(session);
        return modelMapper.map(saved, RideSessionDetailDTO.class);
    }

    @Override
    public void deleteSession(Long id) {
        RideSession session = rideSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt gửi xe với ID: " + id));

        if (session.getStationDock() != null && session.getStationDock().getStatus() == StationDockStatus.OCCUPIED) {
            dockAvailabilityService.updateDockStatus(session.getStationDock().getId(), StationDockStatus.AVAILABLE);
        }
        if (session.getFeedbacks() != null && !session.getFeedbacks().isEmpty()) {
            feedbackRepository.deleteAll(session.getFeedbacks());
        }
        if (session.getPayments() != null && !session.getPayments().isEmpty()) {
            paymentRepository.deleteAll(session.getPayments());
        }
        session.setBooking(null);
        rideSessionRepository.delete(session);
    }
}