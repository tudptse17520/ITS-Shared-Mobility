package vn.edu.ut.its.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.edu.ut.its.constant.StationStatus;

import java.util.List;

@Entity
@Table(name = "station")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "station_name", nullable = false, columnDefinition = "nvarchar(100)")
    private String stationName;

    @Column(nullable = false, columnDefinition = "nvarchar(255)")
    private String address;

    @Column(name = "number_of_zones", nullable = false)
    private Integer numberOfZones;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "total_docks")
    private Integer totalDocks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StationStatus status = StationStatus.ACTIVE;

    // ==================== Relationships ====================

    @OneToMany(mappedBy = "station", fetch = FetchType.LAZY)
    private List<StationZone> stationZones;
}

