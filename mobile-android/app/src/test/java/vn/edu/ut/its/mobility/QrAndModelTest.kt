package vn.edu.ut.its.mobility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.edu.ut.its.mobility.model.FeedbackRequestDto
import vn.edu.ut.its.mobility.model.FleetVehicleDto
import vn.edu.ut.its.mobility.ui.extractVehicleCodeFromQr

class QrAndModelTest {

    @Test
    fun extractVehicleCodeFromQr_handlesPlainCodeAndWhitespace() {
        assertEquals("BIKE-001", extractVehicleCodeFromQr("  bike-001  "))
        assertEquals("", extractVehicleCodeFromQr("   "))
    }

    @Test
    fun extractVehicleCodeFromQr_handlesJsonPayload() {
        assertEquals("EV-102", extractVehicleCodeFromQr("""{"code":"ev-102","stationId":1}"""))
        assertEquals("SC-204", extractVehicleCodeFromQr("""{"vehicleCode":"sc-204"}"""))
    }

    @Test
    fun extractVehicleCodeFromQr_handlesQueryUrl() {
        assertEquals("ITS-99", extractVehicleCodeFromQr("https://its.ut.edu.vn/unlock?code=its-99&station=2"))
        assertEquals("BIKE-05", extractVehicleCodeFromQr("its://scan?foo=1&CODE=bike-05"))
    }

    @Test
    fun feedbackRequestDto_defaultsStatusToReported() {
        val dto = FeedbackRequestDto(
            rideSessionId = 42L,
            issueType = "OTHER",
            description = "Phanh hơi lỏng"
        )
        assertEquals(42L, dto.rideSessionId)
        assertEquals("REPORTED", dto.status)
    }

    @Test
    fun fleetVehicleDto_storesTelemetryAndPricingFields() {
        val v = FleetVehicleDto(
            id = 1L,
            vehicleCode = "BIKE-001",
            plate = "59-MD1 001.01",
            brand = "VinFast",
            color = "Xanh",
            vehicleTypeId = 1L,
            vehicleTypeName = "Xe máy điện",
            currentStationId = 1L,
            currentStationName = "Trạm UTH",
            latitude = 10.8045,
            longitude = 106.7166,
            batteryLevel = 88,
            lockStatus = "LOCKED",
            fleetStatus = "AVAILABLE",
            distanceMeters = 120.0,
            unlockFee = 10000.0,
            hourlyRate = 25000.0
        )
        assertEquals("AVAILABLE", v.fleetStatus)
        assertTrue(v.batteryLevel in 0..100)
    }
}
