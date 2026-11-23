package vn.edu.ut.its.entity;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.edu.ut.its.constant.StationDockStatus;

@Entity
@Table(name = "station_dock")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationDock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dock_name", nullable = false, columnDefinition = "nvarchar(100)")
    private String dockName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StationDockStatus status = StationDockStatus.AVAILABLE;

    // ==================== Relationships ====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_zone_id")
    private StationZone stationZone;

    @OneToMany(mappedBy = "stationDock", fetch = FetchType.LAZY)
    private List<Booking> bookings;

    @OneToMany(mappedBy = "stationDock", fetch = FetchType.LAZY)
    private List<RideSession> rideSessions;
}
