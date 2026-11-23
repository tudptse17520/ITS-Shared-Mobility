import axiosClient from "@/lib/axios-client";
import type { LoginRequest, LoginResponse } from "@/features/auth/types/login.type";

export interface RegisterRequestPayload {
  username: string;
  password: string;
  fullName: string;
  phoneNumber: string;
}

export const authService = {
  login: async (data: LoginRequest): Promise<LoginResponse> => {
    const response = await axiosClient.post<LoginResponse>("/auth/login", data);
    return response.data;
  },
  register: async (data: RegisterRequestPayload): Promise<LoginResponse> => {
    const response = await axiosClient.post<LoginResponse>("/auth/register", data);
    return response.data;
  },
  changePassword: async (data: import("@/features/auth/schemas/change-password.schema").ChangePasswordFormValues): Promise<{ success: boolean; message: string }> => {
    const response = await axiosClient.post<{ success: boolean; message: string }>("/auth/change-password", data);
    return response.data;
  },
  getMe: async (): Promise<import("@/stores/auth.store").AuthUser> => {
    const response = await axiosClient.get<import("@/stores/auth.store").AuthUser>("/auth/me");
    return response.data;
  },
};
