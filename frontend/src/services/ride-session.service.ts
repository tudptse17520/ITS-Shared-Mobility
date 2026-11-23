import axiosClient from "@/lib/axios-client";
import { ApiResponse } from "@/types/api";

import {
  CheckOutRequest,
  CheckOutResponse,
  FeeCalculationResponse,
  LostTicketRequest,
  OverrideCheckoutRequest,
  RideSession,
  PlateValidationRequest,
  SessionListResponse,
  SessionResponse,
} from "@/features/ride-sessions/types/ride-session.type";

const SESSION_API = '/ride-sessions';

export const sessionService = {
  getSessions: async (keyword?: string, status?: string): Promise<SessionListResponse> => {
    const params = new URLSearchParams();
    if (keyword) params.append('plate', keyword);
    if (status && status !== 'ALL') params.append('status', status);
    
    const response = await axiosClient.get<SessionListResponse>(SESSION_API, { params });
    return response.data;
  },

  checkInVehicle: async (data: { plate: string; vehicleId?: number | null; stationDockId?: number | null }): Promise<SessionResponse> => {
    const response = await axiosClient.post<SessionResponse>(`${SESSION_API}/check-in`, data);
    return response.data;
  },

  updateSession: async (id: number, data: { plate: string; vehicleId?: number | null; stationDockId?: number | null }): Promise<SessionResponse> => {
    const response = await axiosClient.put<SessionResponse>(`${SESSION_API}/${id}`, data);
    return response.data;
  },

  updateSessionStatus: async (id: number, status: string): Promise<SessionResponse> => {
    const response = await axiosClient.patch<SessionResponse>(`${SESSION_API}/${id}/status`, { status });
    return response.data;
  },

  deleteSession: async (id: number): Promise<void> => {
    await axiosClient.delete(`${SESSION_API}/${id}`);
  },

  getRideSessions: async (params?: {
    plate?: string;
    status?: string;
    from_date?: string;
  }): Promise<{ totalItems: number; data: RideSession[] }> => {
    const { data } = await axiosClient.get<{ totalItems: number; data: RideSession[] }>("/ride-sessions", {
      params,
    });
    return data;
  },

  getUserSessions: async (userId: number): Promise<{ totalItems: number; data: RideSession[] }> => {
    const { data } = await axiosClient.get<{ totalItems: number; data: RideSession[] }>(`/ride-sessions/users/${userId}/sessions`);
    return data;
  },

  validatePlate: async (
    sessionId: number,
    request: PlateValidationRequest
  ): Promise<{ message: string }> => {
    const { data } = await axiosClient.post<{ message: string }>(
      `/ride-sessions/${sessionId}/validate-plate`,
      request
    );
    return data;
  },

  overrideCheckout: async (
    sessionId: number,
    request: OverrideCheckoutRequest
  ): Promise<CheckOutResponse> => {
    const { data } = await axiosClient.post<CheckOutResponse>(
      `/ride-sessions/${sessionId}/override-checkout`,
      request
    );
    return data;
  },

  lostTicket: async (
    sessionId: number,
    request: LostTicketRequest
  ): Promise<FeeCalculationResponse> => {
    const { data } = await axiosClient.post<FeeCalculationResponse>(
      `/ride-sessions/${sessionId}/lost-ticket`,
      request
    );
    return data;
  },

  calculateFee: async (sessionId: number): Promise<FeeCalculationResponse> => {
    const { data } = await axiosClient.post<FeeCalculationResponse>(
      `/ride-sessions/${sessionId}/calculate-fee`
    );
    return data;
  },

  checkExitGate: async (sessionId: number): Promise<{ message: string }> => {
    const { data } = await axiosClient.post<{ message: string }>(
      `/ride-sessions/${sessionId}/exit-gate`
    );
    return data;
  },

  checkOut: async (
    sessionId: number,
    request: CheckOutRequest
  ): Promise<CheckOutResponse> => {
    const { data } = await axiosClient.put<CheckOutResponse>(
      `/ride-sessions/${sessionId}/check-out`,
      request
    );
    return data;
  },
};
