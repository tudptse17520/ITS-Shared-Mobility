export interface ReportFilter {
    startDate?: string; // Định dạng YYYY-MM-DD hoặc ISO
    endDate?: string;   // Định dạng YYYY-MM-DD hoặc ISO
    vehicleTypeId?: number;
}

export interface ReportDetail {
    name?: string;
    date?: string;
    value?: number;
    amount?: number;
    revenue?: number;
    ridesStarted?: number;
    ridesEnded?: number;


    [key: string]: unknown;
}

export interface DashboardReport {
    totalRidesStarted: number;
    totalRidesEnded: number;
    totalRevenue: number;
    activeRides: number;
    occupancyRate: number;
    peakHour: string;
    details: ReportDetail;
}

export interface RevenueReport {
    totalRevenue: number;
    details: ReportDetail[];
}

export interface OccupancyRateReport {
    averageOccupancyRate: number;
    maxOccupancyRate: number;
    details: ReportDetail[];
}

export interface PeakHourReport {
    peakHour: string;
    vehicleCount: number;
    details: ReportDetail[];
}

export interface RideFlowReport {
    totalRidesStarted: number;
    totalRidesEnded: number;
    details: ReportDetail[];
}
