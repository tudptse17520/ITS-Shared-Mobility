"use client";

import { usePathname } from "next/navigation";
import { Sidebar } from "@/components/layout/sidebar";
import { Topbar } from "@/components/layout/topbar";
import DriverLayout from "@/app/(driver)/layout";

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();

  if (pathname.startsWith("/user")) {
    return <DriverLayout>{children}</DriverLayout>;
  }

  return (
    <div className="workspace-backdrop grid h-dvh min-h-0 grid-cols-[minmax(0,1fr)] overflow-hidden md:grid-cols-[218px_minmax(0,1fr)] xl:grid-cols-[234px_minmax(0,1fr)]">
      <Sidebar />
      <div className="flex min-h-0 min-w-0 flex-col">
        <Topbar />
        <main key={pathname} id="noi-dung" className="min-h-0 min-w-0 flex-1 overflow-x-hidden overflow-y-auto [scrollbar-gutter:stable]">
          {children}
        </main>
      </div>
    </div>
  );
}
