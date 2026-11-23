package vn.edu.ut.its.service;

import vn.edu.ut.its.dto.report.DashboardReportDTO;
import vn.edu.ut.its.dto.report.OccupancyRateReportDTO;
import vn.edu.ut.its.dto.report.PeakHourReportDTO;
import vn.edu.ut.its.dto.report.RevenueReportDTO;
import vn.edu.ut.its.dto.report.VehicleEntryExitReportDTO;

public interface ReportService {

    RevenueReportDTO getRevenueReport(String startDate, String endDate);

    VehicleEntryExitReportDTO getVehicleEntryExitReport(String startDate, String endDate, Long vehicleTypeId);

    OccupancyRateReportDTO getOccupancyRateReport(String startDate, String endDate, Long vehicleTypeId);

    PeakHourReportDTO getPeakHourReport(String startDate, String endDate, Long vehicleTypeId);

    DashboardReportDTO getDashboardReport(String startDate, String endDate, Long vehicleTypeId);
}