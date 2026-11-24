"use client";

import Link from "next/link";
import { ArrowUpRightIcon, BatteryWarningIcon, BicycleIcon, BuildingsIcon, MapPinIcon, NavigationArrowIcon, WrenchIcon } from "@phosphor-icons/react/ssr";
import { useQuery } from "@tanstack/react-query";
import { useCallback, useEffect, useState } from "react";
import { motion, useReducedMotion, useSpring, useTransform } from "framer-motion";
import { PageContainer } from "@/components/common/page-container";
import { PageHeader } from "@/components/common/page-header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { buttonVariants } from "@/components/ui/button";
import { VietMapLeaflet } from "@/features/fleet-map/components/vietmap-leaflet";
import { AdminDashboardCharts } from "@/features/reports/components/admin-dashboard-charts";
import { AdminSystemActivity } from "@/features/reports/components/admin-system-activity";
import { mobilityService, type FleetVehicleItem } from "@/services/mobility.service";

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

export default function AdminDashboardPage() {
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

  const selected = vehicles?.find((vehicle) => vehicle.id === selectedId);
  const available = vehicles?.filter((vehicle) => vehicle.fleetStatus === "AVAILABLE").length;
  const inUse = vehicles?.filter((vehicle) => vehicle.fleetStatus === "IN_USE").length;
  const lowBattery = vehicles?.filter((vehicle) => vehicle.fleetStatus === "LOW_BATTERY" || vehicle.batteryLevel < 20).length;
  const maintenance = vehicles?.filter((vehicle) => vehicle.fleetStatus === "MAINTENANCE").length;
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
    { label: "Pin yếu", value: lowBattery, Icon: BatteryWarningIcon, tone: "station" },
    { label: "Đang bảo trì", value: maintenance, Icon: WrenchIcon, tone: "alert" },
  ];

  return (
    <PageContainer>
      <PageHeader
        className="admin-entrance"
        title="Điều hành đội xe"
        description="Theo dõi xe, trạm và chuyến đi tại TP. Hồ Chí Minh."
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
              {selected ? selected.vehicleCode + " · Pin " + selected.batteryLevel + "%" : "TP. Hồ Chí Minh"}
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
            {warnings.map(({ label, value, Icon, tone }) => (
              <div key={label} className="flex items-center gap-3 rounded-lg border bg-background/50 px-3 py-2.5">
                <span data-tone={tone} className="metric-symbol grid size-8 shrink-0 place-items-center rounded-lg"><Icon className="size-4" /></span>
                <span className="flex-1 text-sm text-muted-foreground">{label}</span>
                <span className="font-semibold text-foreground tabular-nums">{value ?? "—"}</span>
              </div>
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

      <div className="admin-entrance grid min-w-0 grid-cols-1 gap-4 [--admin-delay:180ms] xl:grid-cols-3">
        <AdminDashboardCharts />
        <AdminSystemActivity />
      </div>
    </PageContainer>
  );
}
