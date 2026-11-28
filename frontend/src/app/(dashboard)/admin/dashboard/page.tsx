"use client";

import Link from "next/link";
import { format, parseISO } from "date-fns";
import {
  ArrowUpRightIcon,
  BatteryWarningIcon,
  BicycleIcon,
  BuildingsIcon,
  CalendarCheckIcon,
  ClockIcon,
  MapPinIcon,
  NavigationArrowIcon,
  WarningCircleIcon,
  WrenchIcon,
} from "@phosphor-icons/react/ssr";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useCallback, useEffect, useState } from "react";
import { motion, useReducedMotion, useSpring, useTransform } from "framer-motion";
import { PageContainer } from "@/components/common/page-container";
import { PageHeader } from "@/components/common/page-header";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { buttonVariants } from "@/components/ui/button";
import { VietMapLeaflet } from "@/features/fleet-map/components/vietmap-leaflet";
import { AdminDashboardCharts } from "@/features/reports/components/admin-dashboard-charts";
import { AdminSystemActivity } from "@/features/reports/components/admin-system-activity";
import { useDashboardReport } from "@/features/reports/hooks/use-report";
import { useSessionsList } from "@/features/ride-sessions/hooks/use-ride-sessions";
import { RideSessionStatusBadge } from "@/features/ride-sessions/components/ride-session-table";
import { useAllBookings } from "@/features/bookings/hooks/use-bookings";
import { useFeedbacks } from "@/features/feedbacks/hooks/use-feedbacks";
import { SessionStatus } from "@/constants/session-status";
import { getCookie } from "@/utils/storage";
import { mobilityService, type FleetVehicleItem } from "@/services/mobility.service";

const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";
const emptyVehicles: FleetVehicleItem[] = [];

function MetricNumber({ value }: { value: number | undefined }) {
  const reduceMotion = useReducedMotion();
  const motionValue = useSpring(0, { stiffness: 160, damping: 23, mass: 0.65 });
  const display = useTransform(motionValue, (number) => Math.round(number).toLocaleString("vi-VN"));
  useEffect(() => {
    if (value !== undefined) motionValue.set(value);
  }, [value, motionValue]);
  const className = "mt-1 text-3xl font-semibold tracking-tight text-foreground tabular-nums sm:text-4xl";
  if (reduceMotion || value === undefined) return <dd className={className}>{value?.toLocaleString("vi-VN") ?? "—"}</dd>;
  return <motion.dd aria-label={String(value)} className={className}>{display}</motion.dd>;
}

function formatShortDateTime(value?: string | null): string {
  if (!value) return "Đang đi";
  try {
    return format(parseISO(value), "dd/MM/yyyy HH:mm");
  } catch {
    return value;
  }
}

export default function AdminDashboardPage() {
  const queryClient = useQueryClient();
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const selectVehicle = useCallback((vehicle: FleetVehicleItem) => setSelectedId(vehicle.id), []);

  const { data: vehicles, isError: vehiclesFailed } = useQuery({
    queryKey: ["fleet-vehicles"],
    queryFn: mobilityService.getAllVehicles,
    refetchInterval: 5000,
  });
  const { data: stations, isError: stationsFailed } = useQuery({
    queryKey: ["fleet-stations"],
    queryFn: mobilityService.getStations,
    refetchInterval: 5000,
  });
  const { data: sessionsResponse } = useSessionsList();
  const { data: bookings = [] } = useAllBookings();
  const { data: feedbacksResponse } = useFeedbacks(1, 20);
  const { data: reportSummary } = useDashboardReport();

  // Kết nối SSE thời gian thực để làm mới tức thì khi Mobile đặt giữ, mở khóa, di chuyển hoặc kết thúc chuyến đi
  useEffect(() => {
    const token = getCookie("access_token");
    if (!token) return;
    const eventSource = new EventSource(`${API_BASE}/iot/stream?token=${encodeURIComponent(token)}`);
    const refreshLiveQueries = () => {
      queryClient.invalidateQueries({ queryKey: ["fleet-vehicles"] });
      queryClient.invalidateQueries({ queryKey: ["fleet-stations"] });
      queryClient.invalidateQueries({ queryKey: ["sessions"] });
      queryClient.invalidateQueries({ queryKey: ["bookings"] });
      queryClient.invalidateQueries({ queryKey: ["reports"] });
    };
    eventSource.addEventListener("fleet-telemetry", refreshLiveQueries);
    return () => {
      eventSource.removeEventListener("fleet-telemetry", refreshLiveQueries);
      eventSource.close();
    };
  }, [queryClient]);

  const selected = vehicles?.find((vehicle) => vehicle.id === selectedId);
  const available = vehicles?.filter((vehicle) => vehicle.fleetStatus === "AVAILABLE").length;
  const inUse = vehicles?.filter((vehicle) => vehicle.fleetStatus === "IN_USE").length;
  const reserved = vehicles?.filter((vehicle) => vehicle.fleetStatus === "RESERVED").length ?? 0;
  const lowBattery = vehicles?.filter((vehicle) => vehicle.fleetStatus === "LOW_BATTERY" || vehicle.batteryLevel < 20).length;
  const maintenance = vehicles?.filter((vehicle) => vehicle.fleetStatus === "MAINTENANCE").length;

  const allSessions = sessionsResponse?.data ?? [];
  const recentSessions = allSessions.slice(0, 8);
  const awaitingPaymentCount = allSessions.filter((s) => s.status === SessionStatus.AWAITING_PAYMENT).length;
  const completedCount = allSessions.filter((s) => s.status === SessionStatus.COMPLETED).length;
  const openFeedbacksCount = (feedbacksResponse?.data ?? []).filter((f) => f.status !== "RESOLVED").length;
  const activeBookings = bookings.filter((b) => b.status === "CONFIRMED" || b.status === "PENDING");

  const totalRevenue =
    typeof (reportSummary as { totalRevenue?: unknown } | undefined)?.totalRevenue === "number"
      ? ((reportSummary as { totalRevenue: number }).totalRevenue)
      : allSessions
          .filter((s) => s.status === SessionStatus.COMPLETED && s.totalFee != null)
          .reduce((sum, s) => sum + (s.totalFee ?? 0), 0);

  const stationsWithBikes = [...(stations ?? [])]
    .filter((station) => station.availableVehicles > 0)
    .sort((left, right) => right.availableVehicles - left.availableVehicles)
    .slice(0, 4);

  const metrics = [
    { label: "Xe trong đội", value: vehicles?.length, Icon: BicycleIcon, tone: "fleet" },
    { label: "Sẵn sàng", value: available, Icon: MapPinIcon, tone: "ready" },
    { label: "Đang cho thuê", value: inUse, Icon: NavigationArrowIcon, tone: "trip" },
    { label: "Trạm xe", value: stations?.length, Icon: BuildingsIcon, tone: "station" },
  ];
  const warnings = [
    { label: "Đang đặt giữ (15p)", value: reserved, Icon: CalendarCheckIcon, tone: "fleet", href: "/manager/bookings" },
    { label: "Chờ thanh toán", value: awaitingPaymentCount, Icon: ClockIcon, tone: "station", href: "/manager/bookings" },
    { label: "Sự cố chờ xử lý", value: openFeedbacksCount, Icon: WarningCircleIcon, tone: "alert", href: "/manager/feedbacks" },
    { label: "Pin yếu", value: lowBattery, Icon: BatteryWarningIcon, tone: "station", href: "/admin/fleet-map" },
    { label: "Đang bảo trì", value: maintenance, Icon: WrenchIcon, tone: "alert", href: "/admin/fleet-map" },
  ];

  return (
    <PageContainer>
      <PageHeader
        className="admin-entrance"
        title="Điều hành đội xe"
        description="Theo dõi trực tiếp xe, trạm, đặt giữ 15 phút, chuyến đi và sự cố từ ứng dụng di động."
        actions={
          <Link className={buttonVariants({ className: "h-10 px-3.5" })} href="/admin/fleet-map">
            Bản đồ đội xe <ArrowUpRightIcon />
          </Link>
        }
      />

      <dl aria-label="Tình hình đội xe" className="operations-metrics its-stagger admin-entrance [--admin-delay:45ms]">
        {metrics.map(({ label, value, Icon, tone }) => (
          <div key={label} className="min-w-0 px-4 py-3 sm:px-5 sm:py-4">
            <dt className="flex items-center gap-2 text-xs font-medium text-muted-foreground sm:text-sm">
              <span data-tone={tone} className="metric-symbol grid size-7 shrink-0 place-items-center rounded-md"><Icon className="size-4" /></span> {label}
            </dt>
            <MetricNumber value={value} />
          </div>
        ))}
      </dl>

      {(vehiclesFailed || stationsFailed) && <p role="status" className="text-sm text-destructive">Chưa tải được một phần dữ liệu đội xe.</p>}

      <div className="admin-entrance grid min-w-0 items-start gap-4 [--admin-delay:125ms] xl:grid-cols-[minmax(0,1fr)_340px]">
        <Card className="min-w-0 gap-3 p-0">
          <div className="flex flex-wrap items-center justify-between gap-2 px-4 pt-4 sm:px-5">
            <h2 className="text-base font-semibold">Vị trí xe và trạm</h2>
            <span className="text-xs text-muted-foreground">
              {selected ? selected.vehicleCode + " · Pin " + selected.batteryLevel + "%" : "TP. Hồ Chí Minh · Tự động cập nhật"}
            </span>
          </div>
          <div className="p-2 pt-0 sm:p-3 sm:pt-0">
            <VietMapLeaflet
              className="h-[clamp(330px,53dvh,585px)]"
              stations={stations ?? []}
              vehicles={vehicles ?? emptyVehicles}
              selectedVehicleId={selected?.id ?? null}
              onSelectVehicle={selectVehicle}
            />
          </div>
        </Card>

        <Card className="min-w-0 gap-3">
          <CardHeader>
            <CardTitle>Cần chú ý</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-2">
            {warnings.map(({ label, value, Icon, tone, href }) => (
              <Link
                key={label}
                href={href}
                className="flex items-center gap-3 rounded-lg border bg-background/50 px-3 py-2 transition-colors hover:bg-muted/60"
              >
                <span data-tone={tone} className="metric-symbol grid size-8 shrink-0 place-items-center rounded-lg"><Icon className="size-4" /></span>
                <span className="flex-1 text-sm text-muted-foreground">{label}</span>
                <span className="font-semibold text-foreground tabular-nums">{value ?? "—"}</span>
              </Link>
            ))}
          </CardContent>
          <CardHeader className="mt-2 border-t pt-3">
            <CardTitle>Trạm còn xe</CardTitle>
          </CardHeader>
          <CardContent>
            {stations && !stationsWithBikes.length && <p className="py-2 text-sm text-muted-foreground">Chưa có trạm còn xe.</p>}
            <ul className="divide-y">
              {stationsWithBikes.map((station) => (
                <li key={station.id} className="flex items-center justify-between gap-3 py-2.5 text-sm">
                  <span className="min-w-0 leading-snug">{station.stationName}</span>
                  <span className="shrink-0 font-semibold text-primary tabular-nums">{station.availableVehicles} xe</span>
                </li>
              ))}
            </ul>
            <Link className="its-interactive mt-3 inline-flex items-center gap-1.5 rounded text-sm font-medium text-primary hover:underline" href="/manager/stations">
              Xem tất cả trạm <ArrowUpRightIcon className="size-3.5" />
            </Link>
          </CardContent>
        </Card>
      </div>

      <Card className="admin-entrance min-w-0 [--admin-delay:155ms]">
        <CardHeader className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <CardTitle>Lịch sử chuyến đi & Đặt giữ gần đây</CardTitle>
            <CardDescription>
              Tổng {allSessions.length} chuyến ({completedCount} hoàn tất) · Doanh thu thực thu:{" "}
              <strong className="text-foreground tabular-nums">
                {Math.round(totalRevenue).toLocaleString("vi-VN")} đ
              </strong>
            </CardDescription>
          </div>
          <Link
            className="its-interactive inline-flex items-center gap-1.5 rounded text-sm font-medium text-primary hover:underline"
            href="/manager/bookings"
          >
            Xem toàn bộ chuyến đi & đặt giữ <ArrowUpRightIcon className="size-3.5" />
          </Link>
        </CardHeader>
        <CardContent className="space-y-3">
          {activeBookings.length > 0 && (
            <div className="flex flex-wrap items-center gap-2 rounded-lg border border-amber-500/30 bg-amber-500/10 px-3.5 py-2 text-xs text-amber-700 dark:text-amber-300">
              <CalendarCheckIcon className="size-4 shrink-0" />
              <span className="font-medium">Đang giữ chỗ 15 phút ({activeBookings.length}):</span>
              <span>
                {activeBookings
                  .slice(0, 3)
                  .map(
                    (b) =>
                      `${b.vehicleCode || b.licensePlate || `#${b.bookingId}`} (${b.customerName || "Khách thuê"})`,
                  )
                  .join(" · ")}
              </span>
            </div>
          )}

          {recentSessions.length === 0 ? (
            <p className="py-4 text-sm text-muted-foreground">Chưa có chuyến đi nào trên hệ thống.</p>
          ) : (
            <div className="w-full overflow-x-auto rounded-md border">
              <table className="w-full text-left text-sm">
                <thead className="border-b bg-muted/40 text-xs font-medium text-muted-foreground">
                  <tr>
                    <th className="px-3.5 py-2.5">Mã chuyến</th>
                    <th className="px-3.5 py-2.5">Khách thuê</th>
                    <th className="px-3.5 py-2.5">Phương tiện</th>
                    <th className="px-3.5 py-2.5">Quãng đường</th>
                    <th className="px-3.5 py-2.5">Thời gian</th>
                    <th className="px-3.5 py-2.5 text-right">Cước phí</th>
                    <th className="px-3.5 py-2.5">Trạng thái</th>
                  </tr>
                </thead>
                <tbody className="divide-y">
                  {recentSessions.map((session) => (
                    <tr key={session.id} className="transition-colors hover:bg-muted/30">
                      <td className="px-3.5 py-2.5 font-mono text-xs font-semibold text-primary">
                        {session.sessionCode}
                      </td>
                      <td className="px-3.5 py-2.5 font-medium text-foreground">
                        {session.customerName || "Khách thuê"}
                      </td>
                      <td className="px-3.5 py-2.5 text-xs">
                        <span className="font-medium text-foreground">
                          {session.vehicleCode ? `${session.vehicleCode} · ${session.plate}` : session.plate}
                        </span>
                      </td>
                      <td className="px-3.5 py-2.5 tabular-nums text-xs">
                        {(session.distanceKm ?? 0).toFixed(2)} km
                      </td>
                      <td className="px-3.5 py-2.5 text-xs text-muted-foreground">
                        {formatShortDateTime(session.endedAt || session.startedAt)}
                      </td>
                      <td className="px-3.5 py-2.5 text-right font-semibold tabular-nums">
                        {session.totalFee != null
                          ? `${Math.round(session.totalFee).toLocaleString("vi-VN")} đ`
                          : "Đang tính"}
                      </td>
                      <td className="px-3.5 py-2.5">
                        <RideSessionStatusBadge status={session.status} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      <div className="admin-entrance grid min-w-0 grid-cols-1 gap-4 [--admin-delay:180ms] xl:grid-cols-3">
        <AdminDashboardCharts />
        <AdminSystemActivity />
      </div>
    </PageContainer>
  );
}
