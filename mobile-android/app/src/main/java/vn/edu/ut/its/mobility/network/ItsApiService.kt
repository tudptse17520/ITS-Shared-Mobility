package vn.edu.ut.its.mobility.network

import retrofit2.Response
import retrofit2.http.*
import vn.edu.ut.its.mobility.model.*

interface ItsApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>

    @GET("auth/me")
    suspend fun getMe(): Response<UserResponse>

    @GET("mobility/stations")
    suspend fun getStations(): Response<List<StationDto>>

    @GET("mobility/vehicles/nearby")
    suspend fun getNearbyVehicles(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radiusKm") radiusKm: Double = 20.0,
        @Query("onlyAvailable") onlyAvailable: Boolean = false
    ): Response<List<FleetVehicleDto>>

    @POST("mobility/reserve")
    suspend fun reserveVehicle(@Body request: ReserveRequest): Response<BookingResponseDto>

    @POST("mobility/vehicles/{vehicleId}/cancel-reserve")
    suspend fun cancelReservation(@Path("vehicleId") vehicleId: Long): Response<Any>

    @POST("mobility/rides/start")
    suspend fun startRide(@Body request: StartRideRequest): Response<RideSessionDto>

    @POST("mobility/rides/{sessionId}/end")
    suspend fun endRide(
        @Path("sessionId") sessionId: Long,
        @Body request: EndRideRequest
    ): Response<RideSessionDto>

    @GET("mobility/rides/{sessionId}")
    suspend fun getRideSession(@Path("sessionId") sessionId: Long): Response<RideSessionDto>

    @GET("mobility/rides/active")
    suspend fun getActiveRide(@Query("userId") userId: Long? = null): Response<RideSessionDto?>

    @GET("mobility/rides/history")
    suspend fun getRideHistory(@Query("userId") userId: Long? = null): Response<List<RideSessionDto>>

    @POST("iot/simulate-step/{sessionId}")
    suspend fun simulateRideStep(@Path("sessionId") sessionId: Long): Response<RideSessionDto>

    @POST("payments")
    suspend fun createPayment(@Body request: PaymentRequestDto): Response<PaymentResponseDto>
}

