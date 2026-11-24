export interface BookingRequest {
  vehicleId: number;
  stationDockId: number;
  plannedStartAt: string; // ISO DateTime string or DD-MM-YYYY HH:mm:ss based on backend preference
  plannedEndAt: string;
}

export interface BookingResponse {
  id: number;
  status: string;
  message: string;
}

export interface BookingListResponse {
  bookingId: number;
  stationDockId: number;
  stationDockName?: string;
  plannedStartAt: string;
  status: string;
}

export const BOOKING_STATUS = {
  PENDING: "PENDING",
  CONFIRMED: "CONFIRMED",
  USED: "USED",
  CANCELLED: "CANCELLED",
  EXPIRED: "EXPIRED",
} as const;

export type BookingStatus = typeof BOOKING_STATUS[keyof typeof BOOKING_STATUS];
