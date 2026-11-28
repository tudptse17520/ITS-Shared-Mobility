export interface BookingResponse {
  id: number;
  status: string;
  message: string;
}

export interface BookingListResponse {
  bookingId: number;
  stationDockId: number | null;
  stationDockName?: string;
  stationName?: string;
  createdAt?: string;
  expiredAt?: string;
  plannedStartAt: string;
  plannedEndAt?: string;
  customerName?: string;
  customerPhone?: string;
  vehicleCode?: string;
  licensePlate?: string;
  vehicleTypeName?: string;
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
