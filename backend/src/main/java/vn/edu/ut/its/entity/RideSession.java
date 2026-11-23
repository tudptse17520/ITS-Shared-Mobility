package vn.edu.ut.its.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.edu.ut.its.constant.RideSessionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity đại diện cho bảng ride_session trong SQL Server.
 * Ghi nhận thời gian, phương tiện và cước của một chuyến đi.
 */
@Entity
@Table(name = "ride_session")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_code", nullable = false, unique = true, length = 50)
    private String sessionCode;

    @Column(nullable = false, length = 20)
    private String plate;

    @Column(name = "time_in", nullable = false)
    private LocalDateTime timeIn;

    @Column(name = "time_out")
    private LocalDateTime timeOut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RideSessionStatus status = RideSessionStatus.IN_PROGRESS;

    @Column(name = "total_fee", precision = 10, scale = 2)
    private BigDecimal totalFee;

    @Column(name = "start_latitude")
    private Double startLatitude;

    @Column(name = "start_longitude")
    private Double startLongitude;

    @Column(name = "end_latitude")
    private Double endLatitude;

    @Column(name = "end_longitude")
    private Double endLongitude;

    @Column(name = "distance_km")
    @Builder.Default
    private Double distanceKm = 0.0;

    @Column(name = "start_battery_level")
    private Integer startBatteryLevel;

    @Column(name = "end_battery_level")
    private Integer endBatteryLevel;

    // ==================== Relationships ====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_dock_id")
    private StationDock stationDock;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @OneToMany(mappedBy = "rideSession", fetch = FetchType.LAZY)
    private List<Payment> payments;

    @OneToMany(mappedBy = "rideSession", fetch = FetchType.LAZY)
    private List<Feedback> feedbacks;
}
