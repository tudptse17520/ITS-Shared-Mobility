// ---------------------------------------------
// Theme Provider
// Quản lý giao diện Sáng / Tối (Light / Dark mode)
// ---------------------------------------------

"use client";

import { useEffect } from "react";
import { useThemeStore } from "@/stores/theme.store";

interface ThemeProviderProps {
  children: React.ReactNode;
}

export function ThemeProvider({ children }: ThemeProviderProps) {
  const { theme } = useThemeStore();

  useEffect(() => {
    const root = document.documentElement;
    const media = window.matchMedia("(prefers-color-scheme: dark)");
    function applyTheme() {
      const dark = theme === "system" ? media.matches : theme === "dark";
      root.classList.toggle("dark", dark);
      root.dataset.theme = dark ? "dark" : "light";
    }
    applyTheme();
    if (theme === "system") {
      media.addEventListener("change", applyTheme);
      return () => media.removeEventListener("change", applyTheme);
    }
  }, [theme]);

  return <>{children}</>;
}
