package vn.edu.ut.its.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.ut.its.constant.StationDockStatus;
import vn.edu.ut.its.constant.PaymentStatus;
import vn.edu.ut.its.constant.RideSessionStatus;
import vn.edu.ut.its.constant.FeeType;
import vn.edu.ut.its.dto.report.DashboardReportDTO;
import vn.edu.ut.its.dto.report.OccupancyRateReportDTO;
import vn.edu.ut.its.dto.report.PeakHourReportDTO;
import vn.edu.ut.its.dto.report.RevenueReportDTO;
import vn.edu.ut.its.dto.report.RideFlowReportDTO;
import vn.edu.ut.its.entity.RideSession;
import vn.edu.ut.its.entity.Payment;
import vn.edu.ut.its.exception.BusinessRuleViolationException;
import vn.edu.ut.its.repository.StationZoneRepository;
import vn.edu.ut.its.repository.RideSessionRepository;
import vn.edu.ut.its.repository.StationDockRepository;
import vn.edu.ut.its.repository.PaymentRepository;
import vn.edu.ut.its.service.ReportService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final StationDockRepository stationDockRepository;
    private final RideSessionRepository rideSessionRepository;
    private final PaymentRepository paymentRepository;
    private final StationZoneRepository stationZoneRepository;

    @Override
    public RevenueReportDTO getRevenueReport(String startDate, String endDate) {
        LocalDateTime start = parseDate(startDate, false);
        LocalDateTime end = parseDate(endDate, true);

        // Fetch successful payments within date range
        List<Payment> payments = paymentRepository.findByStatus(PaymentStatus.SUCCESS).stream()
                .filter(payment -> payment.getFeeType() != FeeType.HISTORICAL_ADJUSTMENT)
                .filter(p -> !p.getPaymentTime().isBefore(start) && !p.getPaymentTime().isAfter(end))
                .collect(Collectors.toList());

        BigDecimal totalRevenue = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Create breakdown details
        List<Map<String, Object>> details = new ArrayList<>();

        // Group by Date
        Map<String, BigDecimal> dailyRevenue = payments.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getPaymentTime().toLocalDate().toString(),
                        Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)
                ));

        dailyRevenue.forEach((date, amt) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("type", "daily");
            item.put("date", date);
            item.put("amount", amt);
            details.add(item);
        });

        // Group by Vehicle Type
        Map<String, BigDecimal> vehicleTypeRevenue = payments.stream()
                .collect(Collectors.groupingBy(
                        p -> {
                            if (p.getRideSession() != null && p.getRideSession().getVehicle() != null) {
                                return p.getRideSession().getVehicle().getVehicleType().getTypeName();
                            } else if (p.getBooking() != null && p.getBooking().getVehicle() != null) {
                                return p.getBooking().getVehicle().getVehicleType().getTypeName();
                            } else {
                                return "Không phân loại";
                            }
                        },
                        Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)
                ));

        vehicleTypeRevenue.forEach((type, amt) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("type", "vehicle_type");
            item.put("vehicle_type", type);
            item.put("amount", amt);
            details.add(item);
        });

        return RevenueReportDTO.builder()
                .totalRevenue(totalRevenue)
                .details(details)
                .build();
    }

    @Override
    public RideFlowReportDTO getRideFlowReport(String startDate, String endDate, Long vehicleTypeId) {
        LocalDateTime start = parseDate(startDate, false);
        LocalDateTime end = parseDate(endDate, true);
        boolean singleDay = start.toLocalDate().equals(end.toLocalDate());
        Map<String, Long> started = new TreeMap<>();
        Map<String, Long> ended = new TreeMap<>();

        for (RideSession ride : rideSessionRepository.findAll()) {
            if (!matchesVehicleType(ride, vehicleTypeId)) {
                continue;
            }
            if (inRange(ride.getStartedAt(), start, end)) {
                started.merge(reportBucket(ride.getStartedAt(), singleDay), 1L, Long::sum);
            }
            if (inRange(ride.getEndedAt(), start, end)) {
                ended.merge(reportBucket(ride.getEndedAt(), singleDay), 1L, Long::sum);
            }
        }

        Set<String> buckets = new TreeSet<>(started.keySet());
        buckets.addAll(ended.keySet());
        List<Map<String, Object>> details = new ArrayList<>();
        for (String bucket : buckets) {
            details.add(Map.of("date", bucket,
                    "ridesStarted", started.getOrDefault(bucket, 0L),
                    "ridesEnded", ended.getOrDefault(bucket, 0L)));
        }

        return RideFlowReportDTO.builder()
                .totalRidesStarted(started.values().stream().mapToInt(Long::intValue).sum())
                .totalRidesEnded(ended.values().stream().mapToInt(Long::intValue).sum())
                .details(details)
                .build();
    }

    private boolean matchesVehicleType(RideSession ride, Long vehicleTypeId) {
        if (vehicleTypeId == null) {
            return true;
        }
        if (ride.getVehicle() != null && ride.getVehicle().getVehicleType() != null) {
            return vehicleTypeId.equals(ride.getVehicle().getVehicleType().getId());
        }
        return ride.getStationDock() != null && ride.getStationDock().getStationZone() != null
                && ride.getStationDock().getStationZone().getVehicleType() != null
                && vehicleTypeId.equals(ride.getStationDock().getStationZone().getVehicleType().getId());
    }

    private boolean inRange(LocalDateTime timestamp, LocalDateTime start, LocalDateTime end) {
        return timestamp != null && !timestamp.isBefore(start) && !timestamp.isAfter(end);
    }

    private String reportBucket(LocalDateTime timestamp, boolean singleDay) {
        return singleDay ? String.format("%02d:00", timestamp.getHour()) : timestamp.toLocalDate().toString();
    }


    @Override
    public OccupancyRateReportDTO getOccupancyRateReport(String startDate, String endDate, Long vehicleTypeId) {
        // Find current dock occupancy snapshot
        long totalDocks = stationDockRepository.findAll().stream()
                .filter(s -> vehicleTypeId == null || (s.getStationZone() != null && s.getStationZone().getVehicleType() != null && s.getStationZone().getVehicleType().getId().equals(vehicleTypeId)))
                .count();

        long occupiedDocks = stationDockRepository.findAll().stream()
                .filter(s -> s.getStatus() == StationDockStatus.OCCUPIED)
                .filter(s -> vehicleTypeId == null || (s.getStationZone() != null && s.getStationZone().getVehicleType() != null && s.getStationZone().getVehicleType().getId().equals(vehicleTypeId)))
                .count();

        double averageRate = totalDocks > 0 ? (occupiedDocks * 100.0) / totalDocks : 0.0;


        List<Map<String, Object>> details = stationZoneRepository.findAll().stream()
                .filter(f -> vehicleTypeId == null || (f.getVehicleType() != null && f.getVehicleType().getId().equals(vehicleTypeId)))
                .map(f -> {
                    long fTotal = f.getStationDocks() != null ? f.getStationDocks().size() : 0;
                    long fOccupied = f.getStationDocks() != null ? f.getStationDocks().stream().filter(s -> s.getStatus() == StationDockStatus.OCCUPIED).count() : 0;
                    double fRate = fTotal > 0 ? (fOccupied * 100.0) / fTotal : 0.0;

                    Map<String, Object> zoneData = new HashMap<>();
                    zoneData.put("zone_name", f.getZoneName());
                    zoneData.put("total_docks", fTotal);
                    zoneData.put("occupied_docks", fOccupied);
                    zoneData.put("occupancy_rate", fRate);
                    return zoneData;
                })
                .collect(Collectors.toList());

        double maxRate = details.stream()
                .mapToDouble(zone -> ((Number) zone.get("occupancy_rate")).doubleValue())
                .max().orElse(0.0);
        return OccupancyRateReportDTO.builder()
                .averageOccupancyRate(averageRate)
                .maxOccupancyRate(maxRate)
                .details(details)
                .build();
    }

    @Override
    public PeakHourReportDTO getPeakHourReport(String startDate, String endDate, Long vehicleTypeId) {
        LocalDateTime start = parseDate(startDate, false);
        LocalDateTime end = parseDate(endDate, true);

        List<RideSession> sessions = rideSessionRepository.findAll().stream()
                .filter(s -> !s.getStartedAt().isBefore(start) && !s.getStartedAt().isAfter(end))
                .filter(s -> {
                    if (vehicleTypeId == null) return true;
                    if (s.getVehicle() != null && s.getVehicle().getVehicleType() != null) {
                        return s.getVehicle().getVehicleType().getId().equals(vehicleTypeId);
                    }
                    if (s.getStationDock() != null && s.getStationDock().getStationZone() != null && s.getStationDock().getStationZone().getVehicleType() != null) {
                        return s.getStationDock().getStationZone().getVehicleType().getId().equals(vehicleTypeId);
                    }
                    return false;
                })
                .collect(Collectors.toList());

        Map<Integer, Long> hourCounts = sessions.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getStartedAt().getHour(),
                        Collectors.counting()
                ));

        int peakHourVal = -1;
        long maxCount = 0;

        for (Map.Entry<Integer, Long> entry : hourCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                peakHourVal = entry.getKey();
            }
        }

        String peakHourStr = peakHourVal != -1 ? String.format("%02d:00 - %02d:00", peakHourVal, (peakHourVal + 1) % 24) : "N/A";

        List<Map<String, Object>> details = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            Map<String, Object> hMap = new HashMap<>();
            hMap.put("hour", String.format("%02d:00", h));
            hMap.put("vehicle_count", hourCounts.getOrDefault(h, 0L));
            details.add(hMap);
        }

        return PeakHourReportDTO.builder()
                .peakHour(peakHourStr)
                .vehicleCount((int) maxCount)
                .details(details)
                .build();
    }

    @Override
    public DashboardReportDTO getDashboardReport(String startDate, String endDate, Long vehicleTypeId) {
        RevenueReportDTO rev = getRevenueReport(startDate, endDate);
        RideFlowReportDTO flow = getRideFlowReport(startDate, endDate, vehicleTypeId);
        OccupancyRateReportDTO occ = getOccupancyRateReport(startDate, endDate, vehicleTypeId);
        PeakHourReportDTO peak = getPeakHourReport(startDate, endDate, vehicleTypeId);

        Map<String, Object> combinedDetails = new HashMap<>();
        combinedDetails.put("revenue_details", rev.getDetails());
        combinedDetails.put("flow_details", flow.getDetails());
        combinedDetails.put("occupancy_details", occ.getDetails());

        int activeRides = (int) rideSessionRepository.findAll().stream()
                .filter(s -> s.getStatus() == RideSessionStatus.IN_PROGRESS && s.getEndedAt() == null)
                .filter(s -> matchesVehicleType(s, vehicleTypeId))
                .count();

        return DashboardReportDTO.builder()
                .totalRidesStarted(flow.getTotalRidesStarted())
                .totalRidesEnded(flow.getTotalRidesEnded())
                .activeRides(activeRides)
                .totalRevenue(rev.getTotalRevenue())
                .occupancyRate(occ.getAverageOccupancyRate())
                .peakHour(peak.getPeakHour())
                .details(combinedDetails)
                .build();
    }

    private LocalDateTime parseDate(String dateStr, boolean isEnd) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            if (isEnd) {
                return LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
            } else {
                return LocalDateTime.now().minusMonths(1).withHour(0).withMinute(0).withSecond(0);
            }
        }
        String trimmed = dateStr.trim();
        try {
            if (trimmed.contains("-")) {
                String[] parts = trimmed.split("-");
                if (parts[0].length() == 4) { // yyyy-MM-dd
                    return isEnd ? LocalDate.parse(trimmed).atTime(23, 59, 59) : LocalDate.parse(trimmed).atStartOfDay();
                } else { // dd-MM-yyyy
                    return isEnd ? LocalDate.parse(trimmed, DateTimeFormatter.ofPattern("dd-MM-yyyy")).atTime(23, 59, 59) : LocalDate.parse(trimmed, DateTimeFormatter.ofPattern("dd-MM-yyyy")).atStartOfDay();
                }
            }
            return isEnd ? LocalDateTime.now().withHour(23).withMinute(59).withSecond(59) : LocalDateTime.now().minusMonths(1).withHour(0).withMinute(0).withSecond(0);
        } catch (Exception e) {
            throw new BusinessRuleViolationException("Định dạng ngày '" + dateStr + "' không hợp lệ. Vui lòng sử dụng DD-MM-YYYY hoặc YYYY-MM-DD.");
        }
    }
}
