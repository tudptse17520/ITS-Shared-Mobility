package vn.edu.ut.its.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.edu.ut.its.constant.StationZoneStatus;

import java.util.List;

@Entity
@Table(name = "stationZone")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zone_name", nullable = false, columnDefinition = "nvarchar(100)")
    private String zoneName;

    @Column(name = "zone_level", nullable = false)
    private Integer zoneLevel;

    @Column(nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StationZoneStatus status = StationZoneStatus.ACTIVE;

    // ==================== Relationships ====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private Station station;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id")
    private VehicleType vehicleType;

    @OneToMany(mappedBy = "stationZone", fetch = FetchType.LAZY)
    private List<StationDock> stationDocks;
}
