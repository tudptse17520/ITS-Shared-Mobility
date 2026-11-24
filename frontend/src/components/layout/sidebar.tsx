"use client";

import Link from "next/link";
import { LayoutGroup, motion, useReducedMotion } from "framer-motion";
import { useId } from "react";
import { usePathname, useRouter } from "next/navigation";
import { SignOutIcon as LogOut } from "@phosphor-icons/react/ssr";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { ADMIN_NAV } from "@/config/navigation";
import { cn } from "@/lib/utils";
import { useAuthStore } from "@/stores/auth.store";
import { removeCookie } from "@/utils/storage";
import { AdminBrand } from "./brand";

export function AdminNavigation({ onNavigate }: { onNavigate?: () => void }) {
  const pathname = usePathname();
  const reducedMotion = useReducedMotion();
  const groupId = useId();

  return (
    <LayoutGroup id={groupId}>
      <nav aria-label="Điều hướng quản trị" className="space-y-5 px-2">
        {ADMIN_NAV.map((group) => (
          <div key={group.label}>
            <h2 className="mb-2 px-3 text-xs font-semibold text-muted-foreground">{group.label}</h2>
            <div className="grid gap-0.5">
              {group.items.map(({ title, href, icon: Icon }) => {
                const active = pathname === href || pathname.startsWith(href + "/");
                return (
                  <Link
                    key={href}
                    href={href}
                    aria-current={active ? "page" : undefined}
                    onClick={onNavigate}
                    className={cn(
                      "its-interactive relative isolate flex min-h-10 items-center gap-2.5 rounded-lg px-3 py-2 text-[13px] font-medium",
                      active ? "text-sidebar-primary-foreground" : "text-sidebar-foreground hover:bg-sidebar-accent",
                    )}
                  >
                    {active && (
                      <motion.span
                        aria-hidden="true"
                        layoutId={reducedMotion ? undefined : "active-navigation"}
                        transition={{ type: "spring", stiffness: 430, damping: 38 }}
                        className="absolute inset-0 -z-10 rounded-lg bg-sidebar-primary"
                      />
                    )}
                    <Icon className="size-[17px] shrink-0" strokeWidth={1.8} />
                    {title}
                  </Link>
                );
              })}
            </div>
          </div>
        ))}
      </nav>
    </LayoutGroup>
  );
}

export function Sidebar() {
  const router = useRouter();
  const clearUser = useAuthStore((state) => state.clearUser);

  function logout() {
    removeCookie("access_token");
    clearUser();
    router.replace("/login");
  }

  return (
    <aside className="operations-sidebar hidden min-h-0 flex-col border-r px-2.5 py-3 md:flex">
      <div className="mx-2 mb-4 border-b px-1 pt-1.5 pb-4">
        <AdminBrand />
      </div>
      <ScrollArea className="min-h-0 flex-1">
        <AdminNavigation />
      </ScrollArea>
      <div className="mx-2 mt-3 border-t pt-3">
        <Button variant="ghost" className="h-10 w-full justify-start px-3 text-muted-foreground hover:bg-sidebar-accent hover:text-sidebar-foreground" onClick={logout}>
          <LogOut /> Đăng xuất
        </Button>
      </div>
    </aside>
  );
}
