"use client";

import { PageContainer } from "@/components/common/page-container";
import { PageHeader } from "@/components/common/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { buttonVariants } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Users, UserCheck, Bike, MapPin, Circle, Settings, ShieldCheck, UserPlus } from "lucide-react";
import { AdminDashboardCharts } from "@/features/reports/components/admin-dashboard-charts";
import { AdminSystemActivity } from "@/features/reports/components/admin-system-activity";
import Link from "next/link";
import { useUsers } from "@/features/users/hooks/use-users";
import { UserResponse } from "@/types/user.type";

import { useStations } from "@/features/stations/hooks/use-stations";
import { mobilityService } from "@/services/mobility.service";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";

export default function AdminDashboardPage() {
  const queryClient = useQueryClient();

  useEffect(() => {
    const interval = setInterval(() => {
      queryClient.invalidateQueries({ queryKey: ["users"] });
      queryClient.invalidateQueries({ queryKey: ["stations"] });
      queryClient.invalidateQueries({ queryKey: ["fleet-vehicles"] });
    }, 5000);
    return () => clearInterval(interval);
  }, [queryClient]);

  const { data: usersData } = useUsers();
  const { data: stationsData } = useStations();
  const { data: fleetVehicles = [] } = useQuery({
    queryKey: ["fleet-vehicles"],
    queryFn: () => mobilityService.getAllVehicles(),
  });

  const users = usersData?.data || [];
  const totalUsers = users.length;
  const activeUsers = users.filter((u: UserResponse) => u.status === "ACTIVE").length;

  const totalStations = stationsData?.data?.length || 0;
  const totalVehicles = fleetVehicles.length;
  const availableVehicles = fleetVehicles.filter((v) => v.fleetStatus === "AVAILABLE").length;

  const adminStatCards = [
    {
      key: "total_users",
      label: "Tổng Tài Khoản",
      value: totalUsers.toString(),
      description: "Quản trị viên & Khách thuê",
      icon: Users,
      color: "text-blue-500",
      bg: "bg-blue-500/10",
    },
    {
      key: "active_users",
      label: "Đang Hoạt Động",
      value: activeUsers.toString(),
      description: "Tài khoản trạng thái Active",
      icon: UserCheck,
      color: "text-emerald-500",
      bg: "bg-emerald-500/10",
    },
    {
      key: "stations",
      label: "Tổng Trạm Xe (Hub)",
      value: totalStations.toString(),
      description: "Trạm xe công cộng đang mở",
      icon: MapPin,
      color: "text-purple-500",
      bg: "bg-purple-500/10",
    },
    {
      key: "fleet",
      label: "Đội Xe Chia Sẻ",
      value: totalVehicles.toString(),
      description: `${availableVehicles} xe đang sẵn sàng`,
      icon: Bike,
      color: "text-amber-500",
      bg: "bg-amber-500/10",
    },
  ];

  return (
    <PageContainer>
      <div className="flex items-center justify-between mb-4">
        <PageHeader
          title="Admin Dashboard"
          description="Tổng quan quản trị hệ thống ITS Shared Mobility và hạ tầng trạm xe"
        />
        <Badge variant="outline" className="px-3 py-1 text-sm bg-emerald-500/10 text-emerald-500 border-emerald-500/20">
          <Circle className="w-2 h-2 fill-current mr-2 animate-pulse" />
          System Online
        </Badge>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mt-2">
        {adminStatCards.map((card) => {
          const Icon = card.icon;
          return (
            <Card key={card.key} className="group relative overflow-hidden border border-white/5 bg-background shadow-sm transition-all hover:shadow-md">
              <CardContent className="p-5">
                <div className="flex items-start justify-between">
                  <div className="space-y-2">
                    <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                      {card.label}
                    </p>
                    <p className="text-3xl font-bold tracking-tight text-foreground">
                      {card.value}
                    </p>
                    <p className="text-xs text-muted-foreground/60">
                      {card.description}
                    </p>
                  </div>
                  <div className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-xl ${card.bg}`}>
                    <Icon className={`h-6 w-6 ${card.color}`} />
                  </div>
                </div>
              </CardContent>
            </Card>
          );
        })}
      </div>

      {/* Quick Actions */}
      <div className="mt-6">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <Link href="/admin/users?action=create" className={buttonVariants({ variant: "outline", className: "h-auto py-4 flex flex-col items-center justify-center gap-2 border-border/10 bg-background hover:bg-muted/50" })}>
            <UserPlus className="h-5 w-5 text-blue-500" />
            <span className="text-sm font-medium">Thêm Tài Khoản</span>
          </Link>
          <Link href="/admin/users" className={buttonVariants({ variant: "outline", className: "h-auto py-4 flex flex-col items-center justify-center gap-2 border-border/10 bg-background hover:bg-muted/50" })}>
            <ShieldCheck className="h-5 w-5 text-indigo-500" />
            <span className="text-sm font-medium">Phân Quyền</span>
          </Link>
          <Link href="/admin/settings" className={buttonVariants({ variant: "outline", className: "h-auto py-4 flex flex-col items-center justify-center gap-2 border-border/10 bg-background hover:bg-muted/50" })}>
            <Settings className="h-5 w-5 text-amber-500" />
            <span className="text-sm font-medium">Cấu Hình Hệ Thống</span>
          </Link>
          <Link href="/admin/fleet-map" className={buttonVariants({ variant: "outline", className: "h-auto py-4 flex flex-col items-center justify-center gap-2 border-emerald-500/30 bg-emerald-500/5 hover:bg-emerald-500/15" })}>
            <MapPin className="h-5 w-5 text-emerald-500" />
            <span className="text-sm font-medium">Bản đồ Đội xe & IoT</span>
          </Link>
        </div>
      </div>

      {/* Charts and Activity */}
      <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-3">
        <AdminDashboardCharts />
        <AdminSystemActivity />
      </div>
      
    </PageContainer>
  );
}