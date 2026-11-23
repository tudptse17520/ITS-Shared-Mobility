package vn.edu.ut.its.mobility.model

data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val password: String,
    val fullName: String,
    val phoneNumber: String
)

data class LoginResponse(
    val token: String,
    val message: String,
    val role: String
)

data class UserResponse(
    val id: Long,
    val username: String,
    val fullName: String,
    val phoneNumber: String,
    val role: String,
    val status: String
)

data class StationDto(
    val id: Long,
    val stationName: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val totalDocks: Int,
    val availableVehicles: Int,
    val status: String
)

data class FleetVehicleDto(
    val id: Long,
    val vehicleCode: String,
    val plate: String,
    val brand: String,
    val color: String,
    val vehicleTypeId: Long?,
    val vehicleTypeName: String?,
    val currentStationId: Long?,
    val currentStationName: String?,
    val latitude: Double,
    val longitude: Double,
    val batteryLevel: Int,
    val lockStatus: String,
    val fleetStatus: String,
    val distanceMeters: Double?,
    val unlockFee: Double,
    val hourlyRate: Double
)

data class ReserveRequest(
    val vehicleId: Long,
    val userId: Long? = null,
    val holdMinutes: Int = 15
)

data class BookingResponseDto(
    val id: Long,
    val status: String,
    val message: String
)

data class StartRideRequest(
    val vehicleId: Long? = null,
    val vehicleCode: String? = null,
    val userId: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class EndRideRequest(
    val endLatitude: Double? = null,
    val endLongitude: Double? = null,
    val endStationId: Long? = null
)

data class RideSessionDto(
    val sessionId: Long,
    val sessionCode: String,
    val userId: Long?,
    val username: String?,
    val vehicleId: Long?,
    val vehicleCode: String?,
    val plate: String?,
    val vehicleBrand: String?,
    val vehicleTypeName: String?,
    val startLatitude: Double?,
    val startLongitude: Double?,
    val currentLatitude: Double?,
    val currentLongitude: Double?,
    val endLatitude: Double?,
    val endLongitude: Double?,
    val distanceKm: Double,
    val currentBatteryLevel: Int?,
    val lockStatus: String?,
    val startTime: String?,
    val endTime: String?,
    val durationMinutes: Long,
    val estimatedFee: Double,
    val totalFee: Double?,
    val status: String,
    val paid: Boolean
)

data class PaymentRequestDto(
    val parkingSessionId: Long,
    val amount: Double,
    val paymentMethod: String, // "Vnpay" | "Cash"
    val feeType: String = "PARKING_FEE"
)

data class PaymentResponseDto(
    val id: Long,
    val parkingSessionId: Long?,
    val amount: Double,
    val paymentMethod: String,
    val feeType: String,
    val paymentTime: String?,
    val status: String,
    val paymentUrl: String?
)

