package vn.edu.ut.pbms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity đại diện cho bảng vehicle_telemetry_log trong SQL Server.
 * Lưu lịch sử bản tin IoT giả lập (GPS, % pin, tốc độ, trạng thái khóa) theo thời gian thực.
 */
@Entity
@Table(name = "vehicle_telemetry_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleTelemetryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_session_id")
    private ParkingSession parkingSession;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "battery_level", nullable = false)
    private Integer batteryLevel;

    @Column(name = "speed_kmh")
    @Builder.Default
    private Double speedKmh = 0.0;

    @Column(name = "lock_status", nullable = false, length = 20)
    private String lockStatus;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
}

