import axiosClient from "@/lib/axios-client";
import type {
  RideSession,
  SessionFilterParams,
  SessionListResponse,
} from "@/features/ride-sessions/types/ride-session.type";

const SESSION_API = "/ride-sessions";

export const sessionService = {
  getSessions: async (params?: SessionFilterParams): Promise<SessionListResponse> => {
    const { data } = await axiosClient.get<SessionListResponse>(SESSION_API, { params });
    return data;
  },

  getUserSessions: async (userId: number): Promise<SessionListResponse> => {
    const { data } = await axiosClient.get<SessionListResponse>(
      `${SESSION_API}/users/${userId}/sessions`,
    );
    return data;
  },

  updateSessionStatus: async (id: number, status: string): Promise<RideSession> => {
    const { data } = await axiosClient.patch<RideSession>(`${SESSION_API}/${id}/status`, { status });
    return data;
  },

  deleteSession: async (id: number): Promise<void> => {
    await axiosClient.delete(`${SESSION_API}/${id}`);
  },
};
