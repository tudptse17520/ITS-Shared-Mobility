"use client";

import { useEffect } from "react";
import { useAuthStore } from "@/stores/auth.store";
import { getCookie } from "@/utils/storage";
import { jwtDecode } from "jwt-decode";
import { authService } from "@/services/auth.service";

interface AuthProviderProps {
  children: React.ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const { setUser, clearUser, setHydrated } = useAuthStore();

  useEffect(() => {
    let active = true;
    const fetchUser = async () => {
      const token = getCookie("access_token");

      if (token) {
        try {
          interface DecodedPayload {
            sub?: string;
            role?: string;
          }
          jwtDecode<DecodedPayload>(token);
          const user = await authService.getMe();
          if (active) setUser(user);
        } catch {
          if (active) clearUser();
        }
      } else {
        if (active) clearUser();
      }
      if (active) setHydrated(true);
    };

    fetchUser();
    return () => {
      active = false;
    };
  }, [setUser, clearUser, setHydrated]);

  return <>{children}</>;
}
