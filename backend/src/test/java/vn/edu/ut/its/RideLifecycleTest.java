package vn.edu.ut.its;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import vn.edu.ut.its.constant.FeeType;
import vn.edu.ut.its.constant.PaymentMethod;
import vn.edu.ut.its.constant.PaymentStatus;
import vn.edu.ut.its.dto.mobility.StartRideRequestDTO;
import vn.edu.ut.its.dto.request.PaymentRequestDTO;
import vn.edu.ut.its.exception.BusinessRuleViolationException;
import vn.edu.ut.its.repository.UserRepository;
import vn.edu.ut.its.repository.RideSessionRepository;
import vn.edu.ut.its.service.PaymentService;
import vn.edu.ut.its.service.RideSessionService;
import vn.edu.ut.its.service.ReportService;
import vn.edu.ut.its.service.SharedMobilityService;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "its.demo.seed=true")
class RideLifecycleTest {

    @Autowired
    private UserRepository users;
    @Autowired
    private SharedMobilityService mobility;
    @Autowired
    private PaymentService payments;
    @Autowired
    private RideSessionService sessions;
    @Autowired
    private RideSessionRepository rideSessions;
    @Autowired
    private ReportService reports;

    @Test
    void chargesTheFinalFareOnlyAfterTheRideEnds() {
        String today = LocalDate.now().toString();
        var before = reports.getRideFlowReport(today, today, null);
        Long userId = users.findByUsername("user").orElseThrow().getId();
        var started = mobility.startRide(StartRideRequestDTO.builder()
                .userId(userId).vehicleCode("ITS-BK-003").build());
        Long sessionId = started.getSessionId();

        assertEquals(sessionId, mobility.getActiveRideForCurrentUser(userId).getSessionId());
        assertThrows(BusinessRuleViolationException.class, () -> payments.getRemainingDebt(sessionId));
        var prematurePayment = new PaymentRequestDTO(
                sessionId, null, BigDecimal.valueOf(10000), PaymentMethod.Cash, FeeType.RIDE_FEE);
        assertThrows(BusinessRuleViolationException.class, () -> payments.createPayment(prematurePayment, null));

        var ended = mobility.endRide(sessionId, null);
        assertEquals("AWAITING_PAYMENT", ended.getStatus());
        var active = mobility.getActiveRideForCurrentUser(userId);
        assertTrue(active == null || !sessionId.equals(active.getSessionId()));
        var awaitingPayment = sessions.getRideSessions(null, "AWAITING_PAYMENT", null).getData();
        assertTrue(awaitingPayment.stream().anyMatch(s -> sessionId.equals(s.getId())
                && "AWAITING_PAYMENT".equals(s.getStatus())
                && started.getSessionCode().equals(s.getSessionCode())));
        var ride = rideSessions.findById(sessionId).orElseThrow();
        ride.setStartedAt(ride.getStartedAt().minusDays(1));
        rideSessions.save(ride);
        var flow = reports.getRideFlowReport(today, today, null);
        assertEquals(before.getTotalRidesStarted(), flow.getTotalRidesStarted());
        assertEquals(before.getTotalRidesEnded() + 1, flow.getTotalRidesEnded());

        var obsoletePayment = new PaymentRequestDTO(
                sessionId, null, ended.getTotalFee(), PaymentMethod.Cash, FeeType.HISTORICAL_ADJUSTMENT);
        assertThrows(BusinessRuleViolationException.class, () -> payments.createPayment(obsoletePayment, null));

        var fare = payments.getRemainingDebt(sessionId).getRemainingFee();
        assertEquals(0, ended.getTotalFee().compareTo(fare));
        var payment = new PaymentRequestDTO(sessionId, null, fare, PaymentMethod.Cash, FeeType.RIDE_FEE);
        assertEquals(PaymentStatus.SUCCESS, payments.createPayment(payment, null).getStatus());
        assertEquals("COMPLETED", mobility.getRideSession(sessionId).getStatus());
        assertEquals(0, payments.getRemainingDebt(sessionId).getRemainingFee().signum());
    }
}
