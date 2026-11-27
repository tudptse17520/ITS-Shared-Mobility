"use client";

import React, { useSyncExternalStore } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { MoonIcon as Moon, SunIcon as Sun, SignOutIcon as LogOut } from "@phosphor-icons/react/ssr";
import { cn } from "@/lib/utils";
import { useAuthStore } from "@/stores/auth.store";
import { useThemeStore } from "@/stores/theme.store";
import { getDriverNavItems } from "@/config/menu";
import { removeCookie } from "@/utils/storage";
import { Button } from "@/components/ui/button";
import { BrandMark } from "@/components/layout/brand";

const emptySubscribe = () => () => {};

export default function DriverLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const pathname = usePathname();
  const { user } = useAuthStore();
  const { setTheme } = useThemeStore();
  const navItems = getDriverNavItems();

  const mounted = useSyncExternalStore(
    emptySubscribe,
    () => true,
    () => false
  );

  const toggleTheme = () => {
    setTheme(document.documentElement.classList.contains("dark") ? "light" : "dark");
  };

  const handleLogout = () => {
    removeCookie("access_token");
    useAuthStore.getState().clearUser();
    window.location.href = "/login";
  };

  return (
    <div className="workspace-backdrop min-h-dvh">
      {/* Top Navbar — Glass effect */}
      <nav className="workspace-glass sticky top-0 z-30 mx-2.5 mt-2.5 rounded-[20px] border sm:mx-4 sm:mt-4">
        <div className="mx-auto flex h-14 max-w-none items-center justify-between px-4 sm:px-10">
          {/* Logo */}
          <Link href="/profile" className="flex items-center gap-2.5">
            <BrandMark className="size-8" />
            <span className="text-base font-bold tracking-tight">ITS Mobility</span>
          </Link>

          {/* Nav Links */}
          <div className="hidden items-center gap-1 sm:flex">
            {navItems.map((item) => {
              const isActive = pathname === item.href || pathname.startsWith(item.href + "/");
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={cn(
                    "flex items-center gap-2 rounded-lg px-3 py-2 text-sm transition-colors duration-200",
                    isActive
                      ? "bg-primary/10 text-primary font-medium"
                      : "text-muted-foreground hover:bg-muted hover:text-foreground"
                  )}
                >
                  <item.icon className="h-4 w-4" />
                  <span>{item.title}</span>
                </Link>
              );
            })}
          </div>

          {/* Right side */}
          <div className="flex items-center gap-2">
            {mounted && (
              <>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={toggleTheme}
                  className="h-8 w-8 rounded-lg"
                  aria-label="Đổi giao diện sáng tối"
                >
                  <Sun className="size-4 dark:hidden" />
                  <Moon className="hidden size-4 dark:block" />
                </Button>

                {user ? (
                  <>
                    <div className="flex items-center gap-2 rounded-lg px-2 py-1.5">
                      <div className="flex h-7 w-7 items-center justify-center rounded-full bg-primary/10 text-primary ring-2 ring-primary/20">
                        <span className="text-xs font-semibold">
                          {(user.fullName || user.username || "U").charAt(0).toUpperCase()}
                        </span>
                      </div>
                      <span className="hidden text-sm font-medium sm:inline">
                        {user.fullName || user.username}
                      </span>
                    </div>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={handleLogout}
                      className="h-8 w-8 rounded-lg text-muted-foreground hover:text-rose-600 dark:hover:text-rose-400"
                      aria-label="Đăng xuất"
                    >
                      <LogOut className="h-4 w-4" />
                    </Button>
                  </>
                ) : (
                  <div className="flex items-center gap-1.5">
                    <Link href="/login">
                      <Button variant="ghost" size="sm" className="h-8 rounded-lg text-xs sm:text-sm">
                        Đăng nhập
                      </Button>
                    </Link>
                    <Link href="/register">
                      <Button size="sm" className="h-8 rounded-lg text-xs sm:text-sm">
                        Đăng ký
                      </Button>
                    </Link>
                  </div>
                )}
              </>
            )}
          </div>
        </div>

        {/* Mobile Nav */}
        <div className="flex items-center gap-1 overflow-x-auto px-4 pb-2 sm:hidden">
          {navItems.map((item) => {
            const isActive = pathname === item.href || pathname.startsWith(item.href + "/");
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "flex shrink-0 items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs transition-colors duration-200",
                  isActive
                    ? "bg-primary/10 text-primary font-medium"
                    : "text-muted-foreground hover:bg-muted"
                )}
              >
                <item.icon className="h-3.5 w-3.5" />
                <span>{item.title}</span>
              </Link>
            );
          })}
        </div>
      </nav>

      <main className="mx-auto w-full max-w-7xl min-w-0 p-2.5 sm:p-5">{children}</main>
    </div>
  );
}
