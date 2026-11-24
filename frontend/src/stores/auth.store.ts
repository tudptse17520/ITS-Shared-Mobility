import { create } from "zustand";
import type { Role } from "@/constants/role";

export interface AuthUser {
  username: string;
  role: Role | string;
  id?: number;
  fullName?: string;
  phoneNumber?: string;
  status?: string;
}

interface AuthState {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isHydrated: boolean;
  setUser: (user: AuthUser) => void;
  clearUser: () => void;
  setHydrated: (hydrated: boolean) => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  isHydrated: false,

  setUser: (user) =>
    set({
      user,
      isAuthenticated: true,
      isHydrated: true,
    }),

  clearUser: () =>
    set({
      user: null,
      isAuthenticated: false,
      isHydrated: true,
    }),

  setHydrated: (isHydrated) => set({ isHydrated }),
}));
