"use client";

import { useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { CaretDownIcon as ChevronDown, SignOutIcon as LogOut, ListIcon as Menu, MoonIcon as Moon, GearSixIcon as Settings, SunIcon as Sun, UserCircleIcon as UserRound } from "@phosphor-icons/react/ssr";
import { Button } from "@/components/ui/button";
import { Sheet, SheetContent, SheetTrigger, SheetTitle } from "@/components/ui/sheet";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { DropdownMenu, DropdownMenuContent, DropdownMenuGroup, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import { useThemeStore } from "@/stores/theme.store";
import { useAuthStore } from "@/stores/auth.store";
import { ADMIN_NAV } from "@/config/navigation";
import { removeCookie } from "@/utils/storage";
import { Skeleton } from "@/components/ui/skeleton";
import { AdminBrand } from "./brand";
import { AdminNavigation } from "./sidebar";
import { ProfileDialog } from "./profile-dialog";
import { SettingsDialog } from "./settings-dialog";

export function Topbar() {
  const pathname = usePathname();
  const router = useRouter();
  const clearUser = useAuthStore((state) => state.clearUser);
  const user = useAuthStore((state) => state.user);
  const isHydrated = useAuthStore((state) => state.isHydrated);
  const { setTheme } = useThemeStore();
  const [menuOpen, setMenuOpen] = useState(false);
  const [isProfileOpen, setIsProfileOpen] = useState(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const current = ADMIN_NAV.flatMap(({ items }) => items).find(({ href }) => pathname === href || pathname.startsWith(href + "/"));
  const name = user?.fullName || user?.username || "Tài khoản";
  const initials = name.split(" ").slice(-2).map((part) => part.charAt(0)).join("").toUpperCase();

  function logout() {
    removeCookie("access_token");
    clearUser();
    router.replace("/login");
  }

  return (
    <>
      <header className="z-20 flex h-15 shrink-0 items-center justify-between gap-2 border-b bg-card px-3 md:px-5 xl:px-7">
        <div className="flex min-w-0 items-center gap-2.5">
          <Sheet open={menuOpen} onOpenChange={setMenuOpen}>
            <SheetTrigger render={<Button variant="outline" size="icon" className="size-9 md:hidden" aria-label="Mở điều hướng" />}>
              <Menu />
            </SheetTrigger>
            <SheetContent side="left" className="w-[min(19rem,88vw)] gap-2 rounded-r-[24px] p-3">
              <SheetTitle className="sr-only">Điều hướng quản trị</SheetTitle>
              <div className="px-2 pt-2 pb-3"><AdminBrand /></div>
              <ScrollArea className="min-h-0 flex-1"><AdminNavigation onNavigate={() => setMenuOpen(false)} /></ScrollArea>
              <Button variant="ghost" className="h-10 justify-start text-muted-foreground" onClick={logout}><LogOut /> Đăng xuất</Button>
            </SheetContent>
          </Sheet>
          <div className="flex min-w-0 items-center gap-2 text-sm">
            <span className="hidden text-muted-foreground sm:inline">Quản trị</span>
            <span className="hidden text-muted-foreground/60 sm:inline" aria-hidden="true">/</span>
            <span className="truncate font-semibold">{current?.title || "Tổng quan"}</span>
          </div>
        </div>

        <div className="flex shrink-0 items-center gap-2">
          <Button variant="outline" size="icon" className="relative size-9" aria-label="Đổi giao diện sáng tối" onClick={() => setTheme(document.documentElement.classList.contains("dark") ? "light" : "dark")}>
            <Sun className="transition-[opacity,transform] duration-200 dark:-rotate-45 dark:opacity-0" />
            <Moon className="absolute rotate-45 opacity-0 transition-[opacity,transform] duration-200 dark:rotate-0 dark:opacity-100" />
          </Button>

          <DropdownMenu>
            <DropdownMenuTrigger render={<Button variant="ghost" aria-label={"Tài khoản " + name} className="h-10 gap-2 rounded-xl px-1.5 sm:px-2.5" />}>
              {isHydrated || user ? (
                <>
                  <Avatar className="size-8"><AvatarFallback className="bg-accent font-semibold text-accent-foreground">{initials}</AvatarFallback></Avatar>
                  <span className="hidden max-w-44 truncate text-sm sm:inline">{name}</span>
                  <ChevronDown className="hidden size-3.5 text-muted-foreground sm:block" />
                </>
              ) : (
                <div className="flex items-center gap-2">
                  <Skeleton className="size-8 rounded-full" />
                  <Skeleton className="hidden h-3.5 w-24 sm:block" />
                </div>
              )}
            </DropdownMenuTrigger>
            <DropdownMenuContent className="w-56" align="end">
              <DropdownMenuGroup>
                <DropdownMenuLabel className="font-normal">
                  <span className="block font-semibold">{name}</span>
                  <span className="text-xs text-muted-foreground">{user?.role === "ADMIN" ? "Quản trị viên" : "Khách thuê"}</span>
                </DropdownMenuLabel>
              </DropdownMenuGroup>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={() => setIsProfileOpen(true)}><UserRound /> Hồ sơ cá nhân</DropdownMenuItem>
              <DropdownMenuItem onClick={() => setIsSettingsOpen(true)}><Settings /> Cài đặt tài khoản</DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem className="text-destructive" onClick={logout}><LogOut /> Đăng xuất</DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </header>
      <ProfileDialog isOpen={isProfileOpen} onClose={() => setIsProfileOpen(false)} />
      <SettingsDialog isOpen={isSettingsOpen} onClose={() => setIsSettingsOpen(false)} />
    </>
  );
}
