import type { SessionStatus } from "@/constants/session-status";

export interface RideSession {
  id: number;
  sessionCode: string;
  plate: string;
  vehicleCode?: string | null;
  vehicleTypeName?: string | null;
  customerName?: string | null;
  customerPhone?: string | null;
  distanceKm?: number | null;
  startedAt: string;
  endedAt: string | null;
  totalFee: number | null;
  status: SessionStatus;
  vehicleId: number | null;
  stationDockId: number | null;
}

export interface SessionListResponse {
  totalItems: number;
  data: RideSession[];
  message: string;
}

export interface SessionFilterParams {
  plate?: string;
  status?: SessionStatus;
  fromDate?: string;
}
