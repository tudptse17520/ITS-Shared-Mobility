"use client";

import { usePathname } from "next/navigation";
import { Sidebar } from "@/components/layout/sidebar";
import { Topbar } from "@/components/layout/topbar";
import DriverLayout from "@/app/(driver)/layout";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const pathname = usePathname();

  if (pathname.startsWith("/user")) {
    return <DriverLayout>{children}</DriverLayout>;
  }

  return (
    <div className="flex h-screen overflow-hidden bg-background">
      {/* Sidebar - Cố định bên trái, ẩn trên mobile */}
      <Sidebar />

      {/* Main Area - Phần còn lại bên phải */}
      <div className="flex flex-1 flex-col overflow-hidden md:pl-64">
        {/* Topbar */}
        <Topbar />

        {/* Content Area */}
        <main className="flex-1 overflow-y-auto bg-muted/30 p-4 md:p-6">
          {children}
        </main>
      </div>
    </div>
  );
}
