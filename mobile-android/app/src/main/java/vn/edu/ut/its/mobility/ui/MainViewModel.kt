package vn.edu.ut.its.mobility.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import vn.edu.ut.its.mobility.model.*
import vn.edu.ut.its.mobility.network.RetrofitClient

class MainViewModel : ViewModel() {

    var isAuthenticated by mutableStateOf(false)
        private set
    var currentUser by mutableStateOf<UserResponse?>(null)
        private set

    var stations by mutableStateOf<List<StationDto>>(emptyList())
        private set
    var vehicles by mutableStateOf<List<FleetVehicleDto>>(emptyList())
        private set
    var selectedVehicle by mutableStateOf<FleetVehicleDto?>(null)
        private set
    var activeRide by mutableStateOf<RideSessionDto?>(null)
        private set
    var completedRideForPayment by mutableStateOf<RideSessionDto?>(null)
        private set
    var vnpayPaymentUrl by mutableStateOf<String?>(null)
        private set
    var rideHistory by mutableStateOf<List<RideSessionDto>>(emptyList())
        private set
    var isLoadingHistory by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(false)
        private set
    var statusMessage by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf<String?>(null)

    // Vị trí mặc định: ĐH Giao thông Vận tải TP.HCM (UTH Cơ sở 1 - Bình Thạnh)
    var userLat by mutableStateOf(10.8045)
        private set
    var userLng by mutableStateOf(106.7166)
        private set

    fun clearMessages() {
        statusMessage = null
        errorMessage = null
    }

    fun selectVehicle(vehicle: FleetVehicleDto?) {
        selectedVehicle = vehicle
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            isLoading = true
            clearMessages()
            try {
                val res = RetrofitClient.api.login(LoginRequest(username.trim(), password))
                if (res.isSuccessful && res.body() != null) {
                    val body = res.body()!!
                    RetrofitClient.authToken = body.token
                    isAuthenticated = true
                    statusMessage = body.message
                    loadInitialData()
                } else {
                    errorMessage = parseError(res.errorBody()?.string(), "Đăng nhập thất bại")
                }
            } catch (e: Exception) {
                errorMessage = "Không thể kết nối máy chủ: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    fun register(username: String, password: String, fullName: String, phoneNumber: String) {
        viewModelScope.launch {
            isLoading = true
            clearMessages()
            try {
                val res = RetrofitClient.api.register(
                    RegisterRequest(username.trim(), password, fullName.trim(), phoneNumber.trim())
                )
                if (res.isSuccessful && res.body() != null) {
                    val body = res.body()!!
                    RetrofitClient.authToken = body.token
                    isAuthenticated = true
                    statusMessage = body.message
                    loadInitialData()
                } else {
                    errorMessage = parseError(res.errorBody()?.string(), "Đăng ký thất bại")
                }
            } catch (e: Exception) {
                errorMessage = "Lỗi kết nối: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    fun logout() {
        RetrofitClient.authToken = null
        isAuthenticated = false
        currentUser = null
        activeRide = null
        completedRideForPayment = null
        vnpayPaymentUrl = null
        selectedVehicle = null
        rideHistory = emptyList()
    }

    fun loadInitialData() {
        viewModelScope.launch {
            isLoading = true
            try {
                val meRes = RetrofitClient.api.getMe()
                if (meRes.isSuccessful) {
                    currentUser = meRes.body()
                }

                val stRes = RetrofitClient.api.getStations()
                if (stRes.isSuccessful && stRes.body() != null) {
                    stations = stRes.body()!!
                }

                val vRes = RetrofitClient.api.getNearbyVehicles(userLat, userLng, 25.0, false)
                if (vRes.isSuccessful && vRes.body() != null) {
                    vehicles = vRes.body()!!
                    if (selectedVehicle == null && vehicles.isNotEmpty()) {
                        selectedVehicle = vehicles.first()
                    }
                }

                runCatching {
                    val activeRes = RetrofitClient.api.getActiveRide(currentUser?.id)
                    if (activeRes.isSuccessful && activeRes.body() != null) {
                        val ride = activeRes.body()!!
                        if (ride.endTime == null) {
                            activeRide = ride
                        } else if (!ride.paid) {
                            completedRideForPayment = ride
                        }
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Lỗi tải dữ liệu: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    fun refreshFleet() {
        viewModelScope.launch {
            try {
                val stRes = RetrofitClient.api.getStations()
                if (stRes.isSuccessful && stRes.body() != null) {
                    stations = stRes.body()!!
                }
                val vRes = RetrofitClient.api.getNearbyVehicles(userLat, userLng, 25.0, false)
                if (vRes.isSuccessful && vRes.body() != null) {
                    vehicles = vRes.body()!!
                    selectedVehicle?.let { sel ->
                        selectedVehicle = vehicles.find { it.id == sel.id }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun reserveSelectedVehicle() {
        val vehicle = selectedVehicle ?: return
        viewModelScope.launch {
            isLoading = true
            clearMessages()
            try {
                val res = RetrofitClient.api.reserveVehicle(ReserveRequest(vehicle.id, currentUser?.id))
                if (res.isSuccessful && res.body() != null) {
                    statusMessage = res.body()!!.message
                    refreshFleet()
                } else {
                    errorMessage = parseError(res.errorBody()?.string(), "Đặt giữ xe thất bại")
                }
            } catch (e: Exception) {
                errorMessage = "Lỗi kết nối: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    fun cancelSelectedReservation() {
        val vehicle = selectedVehicle ?: return
        viewModelScope.launch {
            isLoading = true
            clearMessages()
            try {
                val res = RetrofitClient.api.cancelReservation(vehicle.id)
                if (res.isSuccessful) {
                    statusMessage = "Đã hủy đặt giữ xe ${vehicle.vehicleCode}."
                    refreshFleet()
                } else {
                    errorMessage = parseError(res.errorBody()?.string(), "Không thể hủy đặt giữ xe")
                }
            } catch (e: Exception) {
                errorMessage = "Lỗi kết nối: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    fun loadRideHistory() {
        viewModelScope.launch {
            isLoadingHistory = true
            try {
                val res = RetrofitClient.api.getRideHistory(currentUser?.id)
                if (res.isSuccessful && res.body() != null) {
                    rideHistory = res.body()!!
                } else {
                    errorMessage = parseError(res.errorBody()?.string(), "Không thể tải lịch sử chuyến đi")
                }
            } catch (e: Exception) {
                errorMessage = "Lỗi tải lịch sử chuyến đi: ${e.localizedMessage}"
            } finally {
                isLoadingHistory = false
            }
        }
    }

    fun startRideByCode(rawCode: String) {
        val normalizedCode = extractVehicleCodeFromQr(rawCode)
        if (normalizedCode.isBlank()) {
            errorMessage = "Mã xe (QR Code) không hợp lệ."
            return
        }
        viewModelScope.launch {
            isLoading = true
            clearMessages()
            try {
                val res = RetrofitClient.api.startRide(
                    StartRideRequest(
                        vehicleCode = normalizedCode,
                        userId = currentUser?.id,
                        latitude = userLat,
                        longitude = userLng
                    )
                )
                if (res.isSuccessful && res.body() != null) {
                    activeRide = res.body()
                    statusMessage = "Mở khóa xe ${activeRide?.vehicleCode} thành công!"
                    refreshFleet()
                } else {
                    errorMessage = parseError(res.errorBody()?.string(), "Không thể mở khóa xe")
                }
            } catch (e: Exception) {
                errorMessage = "Lỗi kết nối: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    fun simulateIotRideStep() {
        val sessionId = activeRide?.sessionId ?: return
        viewModelScope.launch {
            isLoading = true
            clearMessages()
            try {
                val res = RetrofitClient.api.simulateRideStep(sessionId)
                if (res.isSuccessful && res.body() != null) {
                    activeRide = res.body()
                    statusMessage = "IoT Sim: Đã cập nhật GPS mới (${activeRide?.distanceKm} km)"
                    refreshFleet()
                } else {
                    errorMessage = parseError(res.errorBody()?.string(), "Không thể mô phỏng di chuyển")
                }
            } catch (e: Exception) {
                errorMessage = "Lỗi kết nối: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    fun endCurrentRide() {
        val ride = activeRide ?: return
        viewModelScope.launch {
            isLoading = true
            clearMessages()
            try {
                val res = RetrofitClient.api.endRide(
                    ride.sessionId,
                    EndRideRequest(ride.currentLatitude, ride.currentLongitude)
                )
                if (res.isSuccessful && res.body() != null) {
                    completedRideForPayment = res.body()
                    activeRide = null
                    vnpayPaymentUrl = null
                    statusMessage = "Đã khóa xe! Vui lòng thanh toán cước phí."
                    refreshFleet()
                } else {
                    errorMessage = parseError(res.errorBody()?.string(), "Không thể kết thúc chuyến đi")
                }
            } catch (e: Exception) {
                errorMessage = "Lỗi kết nối: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    fun initiatePayment(method: String) {
        val ride = completedRideForPayment ?: return
        val amount = ride.totalFee ?: ride.estimatedFee
        viewModelScope.launch {
            isLoading = true
            clearMessages()
            try {
                val res = RetrofitClient.api.createPayment(
                    PaymentRequestDto(
                        parkingSessionId = ride.sessionId,
                        amount = amount,
                        paymentMethod = method
                    )
                )
                if (res.isSuccessful && res.body() != null) {
                    val payment = res.body()!!
                    if (method == "Vnpay" && !payment.paymentUrl.isNullOrBlank()) {
                        vnpayPaymentUrl = payment.paymentUrl
                        statusMessage = "Đang mở cổng thanh toán VNPay Sandbox..."
                    } else {
                        completedRideForPayment = null
                        vnpayPaymentUrl = null
                        statusMessage = "Thanh toán thành công! Cảm ơn bạn đã sử dụng ITS Mobility."
                        refreshFleet()
                    }
                } else {
                    errorMessage = parseError(res.errorBody()?.string(), "Tạo giao dịch thất bại")
                }
            } catch (e: Exception) {
                errorMessage = "Lỗi kết nối: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    fun finishVnpayFlow() {
        val sessionId = completedRideForPayment?.sessionId
        viewModelScope.launch {
            if (sessionId != null) {
                try {
                    val res = RetrofitClient.api.getRideSession(sessionId)
                    if (res.isSuccessful && res.body() != null && res.body()!!.paid) {
                        statusMessage = "Thanh toán VNPay thành công!"
                    }
                } catch (_: Exception) {
                }
            }
            completedRideForPayment = null
            vnpayPaymentUrl = null
            refreshFleet()
        }
    }

    private fun parseError(raw: String?, fallback: String): String {
        if (raw.isNullOrBlank()) return fallback
        return try {
            val json = JSONObject(raw)
            json.optString("message").ifBlank { fallback }
        } catch (_: Exception) {
            fallback
        }
    }
}

internal fun extractVehicleCodeFromQr(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    if (trimmed.startsWith("{")) {
        try {
            val json = JSONObject(trimmed)
            val fromJson = json.optString("code").ifBlank { json.optString("vehicleCode") }
            if (fromJson.isNotBlank()) return fromJson.trim().uppercase()
        } catch (_: Exception) {
        }
    }
    val codeIdx = trimmed.indexOf("code=", ignoreCase = true)
    if (codeIdx >= 0) {
        val after = trimmed.substring(codeIdx + 5)
        val endIdx = after.indexOf('&')
        val value = if (endIdx >= 0) after.substring(0, endIdx) else after
        if (value.isNotBlank()) return value.trim().uppercase()
    }
    return trimmed.uppercase()
}

