package vn.edu.ut.pbms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "vehicle")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String plate;

    @Column(nullable = false, columnDefinition = "nvarchar(50)")
    private String brand;

    @Column(nullable = false, columnDefinition = "nvarchar(30)")
    private String color;

    @Column(name = "vehicle_code", unique = true, length = 30)
    private String vehicleCode;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "battery_level")
    @Builder.Default
    private Integer batteryLevel = 100;

    @Column(name = "lock_status", length = 20)
    @Builder.Default
    private String lockStatus = "LOCKED";

    @Column(name = "fleet_status", length = 30)
    @Builder.Default
    private String fleetStatus = "AVAILABLE";

    @Column(name = "current_building_id")
    private Long currentBuildingId;

    @Version
    private Long version;

    // ==================== Relationships ====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id")
    private VehicleType vehicleType;

    @OneToMany(mappedBy = "vehicle", fetch = FetchType.LAZY)
    private List<Booking> bookings;

    @OneToMany(mappedBy = "vehicle", fetch = FetchType.LAZY)
    private List<ParkingSession> parkingSessions;
}
