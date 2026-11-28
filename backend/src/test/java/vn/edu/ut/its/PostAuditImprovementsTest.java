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
import vn.edu.ut.its.constant.FeeType;
import vn.edu.ut.its.constant.PaymentMethod;
import vn.edu.ut.its.constant.PaymentStatus;
import vn.edu.ut.its.entity.Booking;
import vn.edu.ut.its.entity.Payment;
import vn.edu.ut.its.entity.SystemConfig;
import vn.edu.ut.its.entity.User;
import vn.edu.ut.its.entity.Vehicle;
import vn.edu.ut.its.repository.BookingRepository;
import vn.edu.ut.its.repository.PaymentRepository;
import vn.edu.ut.its.repository.SystemConfigRepository;
import vn.edu.ut.its.repository.UserRepository;
import vn.edu.ut.its.repository.VehicleRepository;
import vn.edu.ut.its.service.RuntimeConfigProvider;
import vn.edu.ut.its.util.JwtUtil;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "its.demo.seed=true",
        "app.security.require-auth=true",
        "vnpay.hash-secret=ITS_AUDIT_TEST_SECRET_KEY_2026"
})
@AutoConfigureMockMvc
class PostAuditImprovementsTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SystemConfigRepository systemConfigRepository;
    @Autowired
    private VehicleRepository vehicleRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private RuntimeConfigProvider runtimeConfigProvider;

    private String rawJwtFor(String username) {
        User u = userRepository.findByUsername(username).orElseThrow();
        return jwtUtil.generateToken(u.getUsername(), u.getRole().name());
    }

    private String bearerFor(String username) {
        return "Bearer " + rawJwtFor(username);
    }

    @Test
    void systemConfigValidationReturns404And400() throws Exception {
        String adminToken = bearerFor("admin");
        SystemConfig holdConfig = systemConfigRepository
                .findByConfigKey(RuntimeConfigProvider.KEY_HOLD_DURATION_MINUTES).orElseThrow();
        SystemConfig batteryConfig = systemConfigRepository
                .findByConfigKey(RuntimeConfigProvider.KEY_LOW_BATTERY_THRESHOLD).orElseThrow();
        SystemConfig distanceConfig = systemConfigRepository
                .findByConfigKey(RuntimeConfigProvider.KEY_DISTANCE_RATE_PER_KM).orElseThrow();

        mockMvc.perform(put("/api/v1/configurations/999999")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configValue\":\"10\",\"description\":\"missing\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/v1/configurations/" + holdConfig.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configValue\":\"-5\",\"description\":\"invalid hold\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/configurations/" + batteryConfig.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configValue\":\"99\",\"description\":\"invalid battery\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/configurations/" + distanceConfig.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configValue\":\"abc\",\"description\":\"non numeric\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void systemConfigRuntimeEnforcementWorksForHoldLowBatteryAndDistanceRate() throws Exception {
        String adminToken = bearerFor("admin");
        String userToken = bearerFor("user");

        SystemConfig holdConfig = systemConfigRepository
                .findByConfigKey(RuntimeConfigProvider.KEY_HOLD_DURATION_MINUTES).orElseThrow();
        SystemConfig batteryConfig = systemConfigRepository
                .findByConfigKey(RuntimeConfigProvider.KEY_LOW_BATTERY_THRESHOLD).orElseThrow();
        SystemConfig distanceConfig = systemConfigRepository
                .findByConfigKey(RuntimeConfigProvider.KEY_DISTANCE_RATE_PER_KM).orElseThrow();

        String originalHold = holdConfig.getConfigValue();
        String originalBattery = batteryConfig.getConfigValue();
        String originalDistance = distanceConfig.getConfigValue();

        Vehicle vehicle = vehicleRepository.findAll().stream()
                .filter(v -> "AVAILABLE".equals(v.getFleetStatus()))
                .findFirst()
                .orElseThrow();

        try {
            // The server applies the configured duration even if an older client sends a duration.
            mockMvc.perform(put("/api/v1/configurations/" + holdConfig.getId())
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"configValue\":\"25\",\"description\":\"test 25m\"}"))
                    .andExpect(status().isOk());
            assertEquals(25, runtimeConfigProvider.getHoldDurationMinutes());

            mockMvc.perform(post("/api/v1/mobility/reserve")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"vehicleId\":" + vehicle.getId() + ",\"holdMinutes\":15}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"))
                    .andExpect(jsonPath("$.expiresAtEpochMillis").isNumber());

            Booking latestBooking = bookingRepository.findAll().stream()
                    .filter(b -> b.getVehicle().getId().equals(vehicle.getId()))
                    .max(Comparator.comparing(Booking::getId))
                    .orElseThrow();
            long holdMinutes = Duration.between(latestBooking.getCreatedAt(), latestBooking.getExpiredAt()).toMinutes();
            assertEquals(25L, holdMinutes);

            mockMvc.perform(post("/api/v1/mobility/vehicles/" + vehicle.getId() + "/cancel-reserve")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk());

            // 2. LOW_BATTERY_THRESHOLD = 30 -> telemetry with 25% transitions vehicle to LOW_BATTERY
            mockMvc.perform(put("/api/v1/configurations/" + batteryConfig.getId())
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"configValue\":\"30\",\"description\":\"test 30%\"}"))
                    .andExpect(status().isOk());
            assertEquals(30, runtimeConfigProvider.getLowBatteryThreshold());

            String telemetryBody = objectMapper.writeValueAsString(java.util.Map.of(
                    "vehicleId", vehicle.getId(),
                    "latitude", vehicle.getLatitude(),
                    "longitude", vehicle.getLongitude(),
                    "batteryLevel", 25,
                    "speedKmh", 0.0,
                    "lockStatus", "LOCKED"
            ));
            mockMvc.perform(post("/api/v1/iot/telemetry")
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(telemetryBody))
                    .andExpect(status().isOk());

            Vehicle afterTelemetry = vehicleRepository.findById(vehicle.getId()).orElseThrow();
            assertEquals("LOW_BATTERY", afterTelemetry.getFleetStatus());

            // Restore vehicle battery & status
            mockMvc.perform(post("/api/v1/iot/vehicles/" + vehicle.getId() + "/control")
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"command\":\"RECHARGE_100\"}"))
                    .andExpect(status().isOk());

            // 3. DISTANCE_RATE_PER_KM = 5000 -> verify runtime provider and end-of-ride fee scaling
            mockMvc.perform(put("/api/v1/configurations/" + distanceConfig.getId())
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"configValue\":\"5000\",\"description\":\"test 5000/km\"}"))
                    .andExpect(status().isOk());
            assertEquals(0, new BigDecimal("5000").compareTo(runtimeConfigProvider.getDistanceRatePerKm()));

            MvcResult startRes = mockMvc.perform(post("/api/v1/mobility/rides/start")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"vehicleId\":" + vehicle.getId() + "}"))
                    .andExpect(status().isOk())
                    .andReturn();
            long sessionId = objectMapper.readTree(startRes.getResponse().getContentAsString()).get("sessionId").asLong();

            mockMvc.perform(post("/api/v1/iot/simulate-step/" + sessionId)
                            .header("Authorization", userToken))
                    .andExpect(status().isOk());

            MvcResult endRes = mockMvc.perform(post("/api/v1/mobility/rides/" + sessionId + "/end")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode endedJson = objectMapper.readTree(endRes.getResponse().getContentAsString());
            assertTrue(endedJson.get("distanceKm").asDouble() > 0.0);
            assertTrue(endedJson.get("totalFee").asDouble() > 0.0);
        } finally {
            SystemConfig h = systemConfigRepository.findById(holdConfig.getId()).orElseThrow();
            h.setConfigValue(originalHold);
            systemConfigRepository.save(h);

            SystemConfig b = systemConfigRepository.findById(batteryConfig.getId()).orElseThrow();
            b.setConfigValue(originalBattery);
            systemConfigRepository.save(b);

            SystemConfig d = systemConfigRepository.findById(distanceConfig.getId()).orElseThrow();
            d.setConfigValue(originalDistance);
            systemConfigRepository.save(d);

            mockMvc.perform(post("/api/v1/iot/vehicles/" + vehicle.getId() + "/control")
                    .header("Authorization", adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"command\":\"RECHARGE_100\"}"));
        }
    }

    @Test
    void sseEndpointsRejectUnauthenticatedAndAcceptQueryToken() throws Exception {
        String rawUserJwt = rawJwtFor("user");

        mockMvc.perform(get("/api/v1/iot/stream"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/stations/availability-stream"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/iot/stream").param("token", rawUserJwt))
                .andExpect(request().asyncStarted());

        mockMvc.perform(get("/api/v1/stations/availability-stream").param("token", rawUserJwt))
                .andExpect(request().asyncStarted());
    }

    @Test
    void reportDateRangeIncludesLateNightTimestampAndOpenInViewFalseWorks() throws Exception {
        String adminToken = bearerFor("admin");
        Payment lateNightPayment = paymentRepository.save(Payment.builder()
                .amount(new BigDecimal("12345.00"))
                .paymentMethod(PaymentMethod.Cash)
                .feeType(FeeType.RIDE_FEE)
                .status(PaymentStatus.SUCCESS)
                .paymentTime(LocalDateTime.of(2099, 12, 31, 23, 59, 59, 500_000_000))
                .build());

        try {
            mockMvc.perform(get("/api/v1/reports/revenue")
                            .param("startDate", "2099-12-31")
                            .param("endDate", "2099-12-31")
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalRevenue").value(12345.0));

            String[] adminGetEndpoints = {
                    "/api/v1/mobility/vehicles",
                    "/api/v1/mobility/stations",
                    "/api/v1/stations",
                    "/api/v1/bookings",
                    "/api/v1/pricing-policies",
                    "/api/v1/feedbacks",
                    "/api/v1/users",
                    "/api/v1/reports",
                    "/api/v1/configurations"
            };
            for (String endpoint : adminGetEndpoints) {
                mockMvc.perform(get(endpoint).header("Authorization", adminToken))
                        .andExpect(status().isOk());
            }
        } finally {
            paymentRepository.deleteById(lateNightPayment.getId());
        }
    }
}
