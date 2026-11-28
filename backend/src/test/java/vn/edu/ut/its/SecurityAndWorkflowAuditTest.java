package vn.edu.ut.its;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.edu.ut.its.constant.BookingStatus;
import vn.edu.ut.its.constant.FeeType;
import vn.edu.ut.its.constant.PaymentMethod;
import vn.edu.ut.its.constant.PaymentStatus;
import vn.edu.ut.its.dto.mobility.ReserveVehicleRequestDTO;
import vn.edu.ut.its.dto.mobility.StartRideRequestDTO;
import vn.edu.ut.its.dto.request.PaymentRequestDTO;
import vn.edu.ut.its.entity.Booking;
import vn.edu.ut.its.entity.Payment;
import vn.edu.ut.its.entity.User;
import vn.edu.ut.its.entity.Vehicle;
import vn.edu.ut.its.repository.BookingRepository;
import vn.edu.ut.its.repository.PaymentRepository;
import vn.edu.ut.its.repository.RideSessionRepository;
import vn.edu.ut.its.repository.UserRepository;
import vn.edu.ut.its.repository.VehicleRepository;
import vn.edu.ut.its.service.PaymentService;
import vn.edu.ut.its.service.ReportService;
import vn.edu.ut.its.service.SharedMobilityService;
import vn.edu.ut.its.util.JwtUtil;
import vn.edu.ut.its.util.VnpayUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "its.demo.seed=true",
        "app.security.require-auth=true",
        "vnpay.hash-secret=ITS_AUDIT_TEST_SECRET_KEY_2026"
})
@AutoConfigureMockMvc
class SecurityAndWorkflowAuditTest {

    private static final String TEST_SECRET = "ITS_AUDIT_TEST_SECRET_KEY_2026";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private VehicleRepository vehicleRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private RideSessionRepository rideSessionRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private SharedMobilityService mobilityService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ReportService reportService;

    private String bearerFor(String username) {
        User u = userRepository.findByUsername(username).orElseThrow();
        return "Bearer " + jwtUtil.generateToken(u.getUsername(), u.getRole().name());
    }

    @Test
    void rejectsUnsupportedPaymentMethodsWithoutCreatingTransactions() throws Exception {
        String token = bearerFor("user");
        long paymentCount = paymentRepository.count();

        for (PaymentMethod method : List.of(PaymentMethod.Momo, PaymentMethod.Credit_Card)) {
            PaymentRequestDTO payment = new PaymentRequestDTO(1L, null, BigDecimal.valueOf(10000),
                    method, FeeType.RIDE_FEE);
            mockMvc.perform(post("/api/v1/payments")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payment)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("tiền mặt hoặc VNPay")));
        }

        assertEquals(paymentCount, paymentRepository.count());
    }

    @Test
    void unauthenticatedRequestsAreRejectedWith401Json() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/v1/mobility/vehicles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/v1/reports"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void userRoleIsForbiddenFromAdminEndpoints() throws Exception {
        String userToken = bearerFor("user");
        Long paymentId = paymentRepository.findAll().stream().findFirst().orElseThrow().getId();
        Long vehicleId = vehicleRepository.findByVehicleCode("ITS-EB-001").orElseThrow().getId();

        mockMvc.perform(get("/api/v1/users").header("Authorization", userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/v1/reports").header("Authorization", userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/v1/bookings").header("Authorization", userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(patch("/api/v1/payments/" + paymentId + "/status")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUCCESS\",\"note\":\"Attempt unauthorized manual update\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(post("/api/v1/iot/vehicles/" + vehicleId + "/control")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"command\":\"LOCK\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void enforcesOwnershipAcrossUsersForReservationsRidesAndPayments() throws Exception {
        String userToken = bearerFor("user");
        String otherToken = bearerFor("thanh.tu");
        User owner = userRepository.findByUsername("user").orElseThrow();
        User other = userRepository.findByUsername("thanh.tu").orElseThrow();
        Vehicle vehicle = vehicleRepository.findByVehicleCode("ITS-BK-007").orElseThrow();

        // Reserve as "user" via authenticated HTTP
        mockMvc.perform(post("/api/v1/mobility/reserve")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleId\":" + vehicle.getId() + ",\"holdMinutes\":15}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        try {
            // "thanh.tu" cannot cancel "user"'s reservation
            mockMvc.perform(post("/api/v1/mobility/vehicles/" + vehicle.getId() + "/cancel-reserve")
                            .header("Authorization", otherToken))
                    .andExpect(status().isForbidden());

            // "user" cannot spoof other user's ID in active-ride / history
            mockMvc.perform(get("/api/v1/mobility/rides/active")
                            .param("userId", String.valueOf(other.getId()))
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden());

            // Cancel properly by owner then test active ride isolation
            mockMvc.perform(post("/api/v1/mobility/vehicles/" + vehicle.getId() + "/cancel-reserve")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk());

            MvcResult startRes = mockMvc.perform(post("/api/v1/mobility/rides/start")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"vehicleId\":" + vehicle.getId() + "}"))
                    .andExpect(status().isOk())
                    .andReturn();
            long sessionId = objectMapper.readTree(startRes.getResponse().getContentAsString()).get("sessionId").asLong();

            // Other user cannot view or end or simulate owner's ride
            mockMvc.perform(get("/api/v1/mobility/rides/" + sessionId).header("Authorization", otherToken))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/iot/simulate-step/" + sessionId).header("Authorization", otherToken))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/mobility/rides/" + sessionId + "/end").header("Authorization", otherToken))
                    .andExpect(status().isForbidden());

            // Owner ends ride
            mockMvc.perform(post("/api/v1/mobility/rides/" + sessionId + "/end")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("AWAITING_PAYMENT"));

            // Other user cannot read owner's debt or pay owner's ride
            mockMvc.perform(get("/api/v1/payments/ride-sessions/" + sessionId + "/debt")
                            .header("Authorization", otherToken))
                    .andExpect(status().isForbidden());

            // "user" sees only their own payments via GET /api/v1/payments
            MvcResult listRes = mockMvc.perform(get("/api/v1/payments").header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode data = objectMapper.readTree(listRes.getResponse().getContentAsString()).get("data");
            for (JsonNode p : data) {
                Long rId = p.get("rideSessionId").isNull() ? null : p.get("rideSessionId").asLong();
                if (rId != null) {
                    var session = rideSessionRepository.findById(rId).orElseThrow();
                    assertEquals(owner.getId(), session.getUser().getId());
                }
            }

            // Pay off the test ride so vehicle stays clean
            var debt = paymentService.getRemainingDebt(sessionId);
            paymentService.createPayment(new PaymentRequestDTO(
                    sessionId, null, debt.getRemainingFee(), PaymentMethod.Cash, FeeType.RIDE_FEE), null);
        } finally {
            Vehicle reload = vehicleRepository.findById(vehicle.getId()).orElseThrow();
            if ("RESERVED".equalsIgnoreCase(reload.getFleetStatus())) {
                reload.setFleetStatus("AVAILABLE");
                vehicleRepository.save(reload);
            }
        }
    }

    @Test
    void expiresReservationAndReleasesVehicleAutomatically() {
        User user = userRepository.findByUsername("minh.nguyet").orElseThrow();
        Vehicle vehicle = vehicleRepository.findByVehicleCode("ITS-BK-011").orElseThrow();

        var bookingDto = mobilityService.reserveVehicle(ReserveVehicleRequestDTO.builder()
                .vehicleId(vehicle.getId())
                .userId(user.getId())
                .holdMinutes(10)
                .build());

        Booking booking = bookingRepository.findById(bookingDto.getId()).orElseThrow();
        booking.setExpiredAt(LocalDateTime.now().minusMinutes(5));
        bookingRepository.save(booking);

        int released = mobilityService.releaseExpiredReservations();
        assertTrue(released >= 1);

        Booking expiredBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        Vehicle releasedVehicle = vehicleRepository.findById(vehicle.getId()).orElseThrow();
        assertEquals(BookingStatus.EXPIRED, expiredBooking.getStatus());
        assertEquals("AVAILABLE", releasedVehicle.getFleetStatus());
    }

    @Test
    void enforcesStartRideIdempotencyAndSingleActiveRidePerUser() throws Exception {
        String token = bearerFor("minh.nguyet");
        Vehicle v1 = vehicleRepository.findByVehicleCode("ITS-EB-005").orElseThrow();
        Vehicle v2 = vehicleRepository.findByVehicleCode("ITS-BK-015").orElseThrow();

        MvcResult r1 = mockMvc.perform(post("/api/v1/mobility/rides/start")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleId\":" + v1.getId() + "}"))
                .andExpect(status().isOk())
                .andReturn();
        long s1 = objectMapper.readTree(r1.getResponse().getContentAsString()).get("sessionId").asLong();

        // Double tap on same vehicle returns existing active ride cleanly (idempotent)
        MvcResult r2 = mockMvc.perform(post("/api/v1/mobility/rides/start")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleId\":" + v1.getId() + "}"))
                .andExpect(status().isOk())
                .andReturn();
        long s2 = objectMapper.readTree(r2.getResponse().getContentAsString()).get("sessionId").asLong();
        assertEquals(s1, s2);

        // Starting another vehicle concurrently while already riding is rejected (409)
        mockMvc.perform(post("/api/v1/mobility/rides/start")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleId\":" + v2.getId() + "}"))
                .andExpect(status().isConflict());

        // End ride & pay -> active ride becomes empty (null)
        mockMvc.perform(post("/api/v1/mobility/rides/" + s1 + "/end").header("Authorization", token))
                .andExpect(status().isOk());

        MvcResult activeAfterEnd = mockMvc.perform(get("/api/v1/mobility/rides/active").header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(activeAfterEnd.getResponse().getContentAsString().isBlank());

        var debt = paymentService.getRemainingDebt(s1);
        paymentService.createPayment(new PaymentRequestDTO(
                s1, null, debt.getRemainingFee(), PaymentMethod.Cash, FeeType.RIDE_FEE), null);
    }

    @Test
    void validatesVnpaySignatureRejectsDuplicatesAndBlocksRepeatPayment() throws Exception {
        String token = bearerFor("user");
        Vehicle v = vehicleRepository.findByVehicleCode("ITS-ES-014").orElseThrow();

        MvcResult startRes = mockMvc.perform(post("/api/v1/mobility/rides/start")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleId\":" + v.getId() + "}"))
                .andExpect(status().isOk())
                .andReturn();
        long sessionId = objectMapper.readTree(startRes.getResponse().getContentAsString()).get("sessionId").asLong();

        mockMvc.perform(post("/api/v1/mobility/rides/" + sessionId + "/end").header("Authorization", token))
                .andExpect(status().isOk());

        var debt = paymentService.getRemainingDebt(sessionId);
        BigDecimal fare = debt.getRemainingFee();

        // Create first VNPay link, then create a second one -> first PENDING must be failed cleanly
        var first = paymentService.createPayment(new PaymentRequestDTO(
                sessionId, null, fare, PaymentMethod.Vnpay, FeeType.RIDE_FEE), null);
        var second = paymentService.createPayment(new PaymentRequestDTO(
                sessionId, null, fare, PaymentMethod.Vnpay, FeeType.RIDE_FEE), null);
        assertEquals(PaymentStatus.FAILED, paymentRepository.findById(first.getId()).orElseThrow().getStatus());
        assertEquals(PaymentStatus.PENDING, paymentRepository.findById(second.getId()).orElseThrow().getStatus());

        // Invalid signature -> RspCode 97
        String vnpAmount = String.valueOf(fare.multiply(new BigDecimal("100")).longValue());
        mockMvc.perform(get("/api/v1/payments/vnpay-ipn")
                        .param("vnp_TxnRef", String.valueOf(second.getId()))
                        .param("vnp_Amount", vnpAmount)
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_SecureHash", "INVALID_HASH"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"RspCode\":\"97\"")));

        // Signed IPN with mismatched amount -> RspCode 04
        Map<String, String> badAmountParams = new HashMap<>();
        badAmountParams.put("vnp_TxnRef", String.valueOf(second.getId()));
        badAmountParams.put("vnp_Amount", "100");
        badAmountParams.put("vnp_ResponseCode", "00");
        String badHash = VnpayUtil.hashAllFields(new HashMap<>(badAmountParams), TEST_SECRET);
        mockMvc.perform(get("/api/v1/payments/vnpay-ipn")
                        .param("vnp_TxnRef", badAmountParams.get("vnp_TxnRef"))
                        .param("vnp_Amount", badAmountParams.get("vnp_Amount"))
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_SecureHash", badHash))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"RspCode\":\"04\"")));

        // Valid signed IPN -> RspCode 00 -> SUCCESS & COMPLETED
        Map<String, String> validParams = new HashMap<>();
        validParams.put("vnp_TxnRef", String.valueOf(second.getId()));
        validParams.put("vnp_Amount", vnpAmount);
        validParams.put("vnp_ResponseCode", "00");
        String validHash = VnpayUtil.hashAllFields(new HashMap<>(validParams), TEST_SECRET);
        mockMvc.perform(get("/api/v1/payments/vnpay-ipn")
                        .param("vnp_TxnRef", validParams.get("vnp_TxnRef"))
                        .param("vnp_Amount", validParams.get("vnp_Amount"))
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_SecureHash", validHash))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"RspCode\":\"00\"")));

        assertEquals(PaymentStatus.SUCCESS, paymentRepository.findById(second.getId()).orElseThrow().getStatus());
        assertEquals("COMPLETED", mobilityService.getRideSession(sessionId).getStatus());

        // Duplicate IPN -> RspCode 02
        mockMvc.perform(get("/api/v1/payments/vnpay-ipn")
                        .param("vnp_TxnRef", validParams.get("vnp_TxnRef"))
                        .param("vnp_Amount", validParams.get("vnp_Amount"))
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_SecureHash", validHash))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"RspCode\":\"02\"")));

        // Attempting a repeat payment on COMPLETED ride -> 400 Bad Request
        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rideSessionId\":" + sessionId + ",\"amount\":" + fare + ",\"paymentMethod\":\"Cash\",\"feeType\":\"RIDE_FEE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reportsExcludeHistoricalAdjustmentsAndCountCrossBoundaryRidesCorrectly() {
        String today = LocalDate.now().toString();
        BigDecimal beforeRevenue = reportService.getRevenueReport(today, today).getTotalRevenue();
        var beforeFlow = reportService.getRideFlowReport(today, today, null);

        User user = userRepository.findByUsername("user").orElseThrow();
        var started = mobilityService.startRide(StartRideRequestDTO.builder()
                .userId(user.getId())
                .vehicleCode("ITS-ES-016")
                .build());
        var ended = mobilityService.endRide(started.getSessionId(), null);

        // Move startedAt to yesterday -> should count in totalRidesEnded today, NOT totalRidesStarted today
        var session = rideSessionRepository.findById(started.getSessionId()).orElseThrow();
        session.setStartedAt(session.getStartedAt().minusDays(1));
        rideSessionRepository.save(session);

        // Insert a legacy HISTORICAL_ADJUSTMENT payment today -> must NOT inflate ride revenue
        paymentRepository.save(Payment.builder()
                .rideSession(session)
                .amount(BigDecimal.valueOf(99000))
                .paymentMethod(PaymentMethod.Cash)
                .feeType(FeeType.HISTORICAL_ADJUSTMENT)
                .status(PaymentStatus.SUCCESS)
                .paymentTime(LocalDateTime.now())
                .build());

        var afterFlow = reportService.getRideFlowReport(today, today, null);
        BigDecimal afterRevenueWithoutRideFee = reportService.getRevenueReport(today, today).getTotalRevenue();
        assertEquals(beforeFlow.getTotalRidesStarted(), afterFlow.getTotalRidesStarted());
        assertEquals(beforeFlow.getTotalRidesEnded() + 1, afterFlow.getTotalRidesEnded());
        assertEquals(0, beforeRevenue.compareTo(afterRevenueWithoutRideFee));

        paymentService.createPayment(new PaymentRequestDTO(
                session.getId(), null, ended.getTotalFee(), PaymentMethod.Cash, FeeType.RIDE_FEE), null);
    }
}

