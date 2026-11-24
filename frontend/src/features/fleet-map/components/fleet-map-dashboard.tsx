"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import toast from "react-hot-toast";
import { QRCodeSVG } from "qrcode.react";
import { PulseIcon as Activity, BatteryChargingIcon as BatteryCharging, BicycleIcon as Bike, LockKeyIcon as Lock, MapPinIcon as MapPin, ArrowsOutIcon as Maximize2, NavigationArrowIcon as Navigation, PlayIcon as Play, QrCodeIcon as QrCode, ArrowsClockwiseIcon as RefreshCw, ShieldWarningIcon as ShieldAlert, SquareIcon as Square, LockKeyOpenIcon as Unlock, WrenchIcon as Wrench, LightningIcon as Zap } from "@phosphor-icons/react/ssr";

import { PageContainer } from "@/components/common/page-container";
import { PageHeader } from "@/components/common/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { VietMapLeaflet } from "./vietmap-leaflet";
import { getCookie } from "@/utils/storage";
import { getApiErrorMessage } from "@/lib/api-error";
import {
  mobilityService,
  type FleetVehicleItem,
  type RideSessionItem,
  type StationItem,
  type TelemetryLogItem,
} from "@/services/mobility.service";

const API_BASE =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";

function statusBadgeClass(status: string): string {
  switch (status) {
    case "AVAILABLE":
      return "bg-emerald-500/15 text-emerald-600 border-emerald-500/30";
    case "IN_USE":
      return "bg-blue-500/15 text-blue-600 border-blue-500/30";
    case "RESERVED":
      return "bg-amber-500/15 text-amber-600 border-amber-500/30";
    case "LOW_BATTERY":
      return "bg-orange-500/15 text-orange-600 border-orange-500/30";
    case "MAINTENANCE":
      return "bg-rose-500/15 text-rose-600 border-rose-500/30";
    default:
      return "bg-slate-500/15 text-slate-600 border-slate-500/30";
  }
}

function fleetLabel(status: string) {
  switch (status) {
    case "AVAILABLE": return "Sẵn sàng";
    case "IN_USE": return "Đang thuê";
    case "RESERVED": return "Đang giữ";
    case "LOW_BATTERY": return "Pin yếu";
    case "MAINTENANCE": return "Bảo trì";
    default: return "Tạm ngưng";
  }
}

export function FleetMapDashboard() {
  const [stations, setStations] = useState<StationItem[]>([]);
  const [vehicles, setVehicles] = useState<FleetVehicleItem[]>([]);
  const [selectedVehicleId, setSelectedVehicleId] = useState<number | null>(null);
  const [telemetryPath, setTelemetryPath] = useState<TelemetryLogItem[]>([]);
  const [activeSimSession, setActiveSimSession] = useState<RideSessionItem | null>(null);
  const [isAutoSimulating, setIsAutoSimulating] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [sseConnected, setSseConnected] = useState(false);
  const [concurrencyResult, setConcurrencyResult] = useState<{
    total: number;
    success: number;
    conflict: number;
    durationMs: number;
  } | null>(null);
  const [isQrModalOpen, setIsQrModalOpen] = useState(false);

  const simTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const fetchData = useCallback(async () => {
    try {
      const [stationList, vehicleList] = await Promise.all([
        mobilityService.getStations(),
        mobilityService.getAllVehicles(),
      ]);
      setStations(stationList);
      setVehicles(vehicleList);
      setSelectedVehicleId((current) => current ?? vehicleList[0]?.id ?? null);
    } catch (error) {
      console.error("Failed to fetch fleet data:", error);
      toast.error("Không thể tải dữ liệu đội xe từ Backend.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  const selectVehicle = useCallback((vehicle: FleetVehicleItem) => setSelectedVehicleId(vehicle.id), []);

  useEffect(() => {
    let active = true;
    const loadInitial = async () => {
      if (active) {
        await fetchData();
      }
    };
    void loadInitial();
    return () => {
      active = false;
    };
  }, [fetchData]);

  // Kết nối SSE để nhận tọa độ GPS & % pin realtime (đính kèm JWT token qua query param)
  useEffect(() => {
    const token = getCookie("access_token");
    if (!token) {
      return;
    }
    const eventSource = new EventSource(`${API_BASE}/iot/stream?token=${encodeURIComponent(token)}`);
    eventSource.onopen = () => setSseConnected(true);
    eventSource.addEventListener("fleet-telemetry", (event) => {
      setSseConnected(true);
      try {
        const data = JSON.parse((event as MessageEvent).data);
        setVehicles((prev) =>
          prev.map((v) =>
            v.id === data.vehicleId
              ? {
                  ...v,
                  latitude: data.latitude ?? v.latitude,
                  longitude: data.longitude ?? v.longitude,
                  batteryLevel: data.batteryLevel ?? v.batteryLevel,
                  lockStatus: data.lockStatus ?? v.lockStatus,
                  fleetStatus: data.fleetStatus ?? v.fleetStatus,
                }
              : v
          )
        );
      } catch {
        // Ignore malformed SSE payload
      }
    });
    eventSource.onerror = () => {
      setSseConnected(false);
    };

    return () => {
      eventSource.close();
    };
  }, []);

  // Tải lịch sử lộ trình GPS của xe đang chọn
  useEffect(() => {
    if (!selectedVehicleId) return;
    mobilityService
      .getTelemetryHistory(selectedVehicleId)
      .then(setTelemetryPath)
      .catch(() => setTelemetryPath([]));
  }, [selectedVehicleId, vehicles]);

  // Dừng Auto-Simulation khi unmount
  useEffect(() => {
    return () => {
      if (simTimerRef.current) clearInterval(simTimerRef.current);
    };
  }, []);

  const selectedVehicle = useMemo(
    () => vehicles.find((v) => v.id === selectedVehicleId) || null,
    [vehicles, selectedVehicleId]
  );

  const stats = useMemo(() => {
    const total = vehicles.length;
    const available = vehicles.filter((v) => v.fleetStatus === "AVAILABLE").length;
    const inUse = vehicles.filter((v) => v.fleetStatus === "IN_USE").length;
    const attention = vehicles.filter(
      (v) => v.fleetStatus === "LOW_BATTERY" || v.fleetStatus === "MAINTENANCE" || v.batteryLevel < 20
    ).length;
    return { total, available, inUse, attention };
  }, [vehicles]);

  const handleControl = async (
    command: "UNLOCK" | "LOCK" | "RECHARGE_100" | "MAINTENANCE" | "SET_AVAILABLE"
  ) => {
    if (!selectedVehicle) return;
    try {
      const updated = await mobilityService.controlVehicle(selectedVehicle.id, command);
      setVehicles((prev) => prev.map((v) => (v.id === updated.id ? updated : v)));
      toast.success(`Đã gửi lệnh IoT [${command}] tới xe ${updated.vehicleCode}`);
    } catch {
      toast.error("Thao tác điều khiển IoT thất bại.");
    }
  };

  const handleStartAutoSimulation = async () => {
    if (!selectedVehicle) return;
    try {
      let session = activeSimSession;
      if (!session || session.vehicleId !== selectedVehicle.id) {
        session = await mobilityService.startRide(selectedVehicle.id);
        setActiveSimSession(session);
      }

      setIsAutoSimulating(true);
      toast.success(`Bắt đầu mô phỏng lộ trình GPS cho xe ${selectedVehicle.vehicleCode}`);

      if (simTimerRef.current) clearInterval(simTimerRef.current);
      simTimerRef.current = setInterval(async () => {
        try {
          const next = await mobilityService.simulateRideStep(session!.sessionId);
          setActiveSimSession(next);
          const logs = await mobilityService.getTelemetryHistory(selectedVehicle.id);
          setTelemetryPath(logs);
        } catch {
          if (simTimerRef.current) clearInterval(simTimerRef.current);
          setIsAutoSimulating(false);
        }
      }, 1500);

      await fetchData();
    } catch (err: unknown) {
      toast.error(getApiErrorMessage(err, "Không thể bắt đầu mô phỏng chuyến đi (xe cần ở trạng thái AVAILABLE)."));
    }
  };

  const handleStopAutoSimulation = async () => {
    if (simTimerRef.current) {
      clearInterval(simTimerRef.current);
      simTimerRef.current = null;
    }
    setIsAutoSimulating(false);

    if (activeSimSession) {
      try {
        const ended = await mobilityService.endRide(activeSimSession.sessionId);
        toast.success(
          `Đã khóa xe ${ended.vehicleCode} · Quãng đường ${ended.distanceKm} km · Cước: ${Number(ended.totalFee || 0).toLocaleString("vi-VN")}đ`
        );
        setActiveSimSession(null);
        await fetchData();
      } catch {
        toast.error("Không thể kết thúc chuyến đi mô phỏng.");
      }
    }
  };

  const handleRunConcurrencyTest = async () => {
    if (!selectedVehicle) return;
    setConcurrencyResult(null);
    const total = 20;
    const t0 = performance.now();
    const results = await Promise.allSettled(
      Array.from({ length: total }, () =>
        mobilityService.reserveVehicle(selectedVehicle.id)
      )
    );
    const durationMs = Math.round(performance.now() - t0);
    const success = results.filter((r) => r.status === "fulfilled").length;
    const conflict = total - success;

    setConcurrencyResult({ total, success, conflict, durationMs });
    toast.success(`Kiểm thử ${total} request đồng thời: ${success} thành công, ${conflict} bị chặn (409 Conflict)`);
    await fetchData();
  };

  const handleCancelReserve = async () => {
    if (!selectedVehicle) return;
    try {
      await mobilityService.cancelReservation(selectedVehicle.id);
      toast.success(`Đã mở lại xe ${selectedVehicle.vehicleCode} về AVAILABLE`);
      await fetchData();
    } catch (err: unknown) {
      toast.error(getApiErrorMessage(err, "Không thể hủy giữ xe."));
    }
  };

  return (
    <PageContainer>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <PageHeader
          title="Bản đồ đội xe"
          description="Vị trí, pin và trạng thái xe tại TP. Hồ Chí Minh."
        />
        <div className="flex items-center gap-2">
          <Badge
            variant="outline"
            className={
              sseConnected
                ? "bg-emerald-500/10 text-emerald-600 border-emerald-500/30"
                : "bg-amber-500/10 text-amber-600 border-amber-500/30"
            }
          >
            <Activity className="w-3.5 h-3.5 mr-1.5" />
            {sseConnected ? "Đang cập nhật" : "Đang kết nối"}
          </Badge>
          <Button variant="outline" size="sm" onClick={fetchData} disabled={isLoading}>
            <RefreshCw className="w-4 h-4 mr-1.5" />
            Làm mới
          </Button>
        </div>
      </div>

      <dl aria-label="Tình hình đội xe" className="operations-metrics its-stagger">
        {[
          { label: "Tổng xe", value: stats.total, Icon: Bike, tone: "fleet" },
          { label: "Sẵn sàng", value: stats.available, Icon: MapPin, tone: "ready" },
          { label: "Đang cho thuê", value: stats.inUse, Icon: Navigation, tone: "trip" },
          { label: "Cần chú ý", value: stats.attention, Icon: ShieldAlert, tone: "alert" },
        ].map(({ label, value, Icon, tone }) => (
          <div key={label} className="px-4 py-3 sm:px-5">
            <dt className="flex items-center gap-2 text-xs font-medium text-muted-foreground sm:text-sm"><span data-tone={tone} className="metric-symbol grid size-7 shrink-0 place-items-center rounded-md"><Icon className="size-4" /></span> {label}</dt>
            <dd className="mt-1 text-3xl font-semibold tracking-tight tabular-nums">{value}</dd>
          </div>
        ))}
      </dl>

      {/* Main Map + IoT Simulator Panel */}
      <div className="grid min-w-0 grid-cols-1 gap-4 xl:grid-cols-[minmax(330px,365px)_minmax(0,1fr)]">
        <Card className="min-w-0 gap-3 xl:col-start-2 xl:row-start-1">
          <CardHeader className="flex flex-wrap items-center justify-between gap-2">
            <CardTitle className="text-base">TP. Hồ Chí Minh</CardTitle>
            <div className="flex flex-wrap gap-2 text-xs">
              <span className="inline-flex items-center gap-1"><span className="h-2.5 w-2.5 rounded-full bg-emerald-500" /> Sẵn sàng</span>
              <span className="inline-flex items-center gap-1"><span className="h-2.5 w-2.5 rounded-full bg-blue-600" /> Đang thuê</span>
              <span className="inline-flex items-center gap-1"><span className="h-2.5 w-2.5 rounded-full bg-amber-500" /> Đã đặt giữ</span>
              <span className="inline-flex items-center gap-1"><span className="h-2.5 w-2.5 rounded-full bg-rose-500" /> Bảo trì</span>
            </div>
          </CardHeader>
          <CardContent>
            <VietMapLeaflet
              className="h-[clamp(365px,62dvh,760px)]"
              stations={stations}
              vehicles={vehicles}
              selectedVehicleId={selectedVehicleId}
              telemetryPath={telemetryPath}
              onSelectVehicle={selectVehicle}
            />
          </CardContent>
        </Card>

        {/* IoT Simulator & Concurrency Control Panel */}
        <aside className="space-y-4 xl:col-start-1 xl:row-start-1" aria-label="Xe đang theo dõi">
          {/* Danh sách xe rút gọn */}
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm">Đội xe ({vehicles.length})</CardTitle>
            </CardHeader>
            <CardContent className="max-h-[13rem] space-y-1.5 overflow-y-auto">
              {vehicles.map((v) => (
                <button
                  key={v.id}
                  type="button"
                  aria-pressed={v.id === selectedVehicleId}
                  onClick={() => selectVehicle(v)}
                  className={`its-interactive flex w-full items-center justify-between gap-2.5 rounded-xl border px-2.5 py-2 text-left text-xs ${
                    v.id === selectedVehicleId
                      ? "border-primary bg-primary/10"
                      : "hover:bg-muted/50"
                  }`}
                >
                  <div>
                    <div className="font-semibold">{v.vehicleCode} · {v.brand}</div>
                    <div className="text-muted-foreground">{v.currentStationName}</div>
                  </div>
                  <div className="shrink-0 text-right">
                    <div className="font-semibold">{v.batteryLevel}%</div>
                    <div className="text-[10px] text-muted-foreground">{fleetLabel(v.fleetStatus)}</div>
                  </div>
                </button>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-base flex items-center gap-2">
                <Zap className="h-4 w-4 text-amber-500" />
                Điều khiển xe
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {selectedVehicle ? (
                <>
                  <div className="rounded-lg border p-3 bg-muted/30 space-y-1.5">
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-sm">{selectedVehicle.vehicleCode}</span>
                      <Badge variant="outline" className={statusBadgeClass(selectedVehicle.fleetStatus)}>
                        {fleetLabel(selectedVehicle.fleetStatus)}
                      </Badge>
                    </div>
                    <p className="text-xs text-muted-foreground">
                      {selectedVehicle.brand} · {selectedVehicle.plate}
                    </p>
                    <div className="grid grid-cols-2 gap-2 pt-1 text-xs">
                      <div>Pin: <strong>{selectedVehicle.batteryLevel}%</strong></div>
                      <div>Khóa: <strong>{selectedVehicle.lockStatus === "LOCKED" ? "Đã khóa" : "Đã mở"}</strong></div>
                      <div className="col-span-2 text-muted-foreground">
                        GPS: {selectedVehicle.latitude?.toFixed(5)}, {selectedVehicle.longitude?.toFixed(5)}
                      </div>
                    </div>
                  </div>

                  {/* Tem Mã QR Mở Khóa Xe (Vehicle QR Tag) */}
                  <div className="rounded-lg border border-teal-500/30 bg-teal-500/5 p-3 flex items-center gap-3">
                    <div className="shrink-0 rounded-md bg-white p-1.5 shadow-xs border">
                      <QRCodeSVG
                        value={`its://unlock?code=${selectedVehicle.vehicleCode}`}
                        size={76}
                        level="M"
                      />
                    </div>
                    <div className="min-w-0 flex-1 space-y-1">
                      <div className="flex items-center gap-1.5 text-xs font-semibold text-teal-700 dark:text-teal-300">
                        <QrCode className="w-3.5 h-3.5" />
                        Mã QR mở khóa
                      </div>
                      <p className="text-[11px] font-mono text-muted-foreground truncate">
                        its://unlock?code={selectedVehicle.vehicleCode}
                      </p>
                      <Button
                        type="button"
                        size="sm"
                        variant="outline"
                        className="mt-1 h-8 max-w-full px-2 text-xs"
                        onClick={() => setIsQrModalOpen(true)}
                      >
                        <Maximize2 className="w-3 h-3 mr-1" />
                        Phóng to mã QR
                      </Button>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-2">
                    <Button size="sm" variant="outline" onClick={() => handleControl("UNLOCK")}>
                      <Unlock className="w-3.5 h-3.5 mr-1.5" /> Mở khóa
                    </Button>
                    <Button size="sm" variant="outline" onClick={() => handleControl("LOCK")}>
                      <Lock className="w-3.5 h-3.5 mr-1.5" /> Khóa xe
                    </Button>
                    <Button size="sm" variant="outline" onClick={() => handleControl("RECHARGE_100")}>
                      <BatteryCharging className="w-3.5 h-3.5 mr-1.5" /> Sạc 100%
                    </Button>
                    <Button size="sm" variant="outline" onClick={() => handleControl("MAINTENANCE")}>
                      <Wrench className="w-3.5 h-3.5 mr-1.5" /> Bảo trì
                    </Button>
                  </div>

                  {selectedVehicle.fleetStatus !== "AVAILABLE" && (
                    <Button
                      size="sm"
                      variant="secondary"
                      className="w-full"
                      onClick={() =>
                        selectedVehicle.fleetStatus === "RESERVED"
                          ? handleCancelReserve()
                          : handleControl("SET_AVAILABLE")
                      }
                    >
                      Cho thuê lại
                    </Button>
                  )}

                  <div className="border-t pt-3 space-y-2">
                    <p className="text-xs font-semibold uppercase text-muted-foreground">
                      Mô phỏng chuyến đi GPS
                    </p>
                    {!isAutoSimulating ? (
                      <Button
                        size="sm"
                        className="w-full"
                        onClick={handleStartAutoSimulation}
                      >
                        <Play className="w-4 h-4 mr-1.5" />
                        Bắt đầu mô phỏng
                      </Button>
                    ) : (
                      <Button
                        size="sm"
                        variant="destructive"
                        className="w-full"
                        onClick={handleStopAutoSimulation}
                      >
                        <Square className="w-4 h-4 mr-1.5" />
                        Dừng mô phỏng
                      </Button>
                    )}

                    {activeSimSession && (
                      <div className="rounded-md bg-blue-500/10 border border-blue-500/20 p-2.5 text-xs space-y-1">
                        <div>Mã chuyến: <strong>{activeSimSession.sessionCode}</strong></div>
                        <div>Quãng đường: <strong>{activeSimSession.distanceKm} km</strong> · Pin: <strong>{activeSimSession.currentBatteryLevel}%</strong></div>
                        <div>Cước tạm tính: <strong>{Number(activeSimSession.estimatedFee || 0).toLocaleString("vi-VN")}đ</strong></div>
                      </div>
                    )}
                  </div>

                  <details className="space-y-2 border-t pt-3">
                    <summary className="cursor-pointer text-sm font-medium">Thử đặt xe cùng lúc</summary>
                    <Button
                      size="sm"
                      variant="outline"
                      className="w-full border-amber-500/50 text-amber-600 hover:bg-amber-500/10"
                      onClick={handleRunConcurrencyTest}
                    >
                      Chạy thử 20 lượt đặt
                    </Button>
                    {concurrencyResult && (
                      <div className="rounded-md bg-muted p-2.5 text-xs space-y-1">
                        <div>Số lượt đặt: <strong>{concurrencyResult.total}</strong></div>
                        <div className="text-emerald-600">Thành công: <strong>{concurrencyResult.success}</strong></div>
                        <div className="text-rose-600">Bị từ chối: <strong>{concurrencyResult.conflict}</strong></div>
                        <div>Thời gian hoàn tất: <strong>{concurrencyResult.durationMs} ms</strong></div>
                      </div>
                    )}
                  </details>
                </>
              ) : (
                <p className="text-sm text-muted-foreground">Chọn xe trên bản đồ hoặc trong danh sách.</p>
              )}
            </CardContent>
          </Card>

        </aside>
      </div>

      {selectedVehicle && (
        <Dialog open={isQrModalOpen} onOpenChange={setIsQrModalOpen}>
          <DialogContent className="sm:max-w-md">
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2">
                <QrCode className="w-5 h-5 text-teal-600" />
                Mã QR · {selectedVehicle.vehicleCode}
              </DialogTitle>
              <DialogDescription>
                {selectedVehicle.brand} · Biển số {selectedVehicle.plate}
              </DialogDescription>
            </DialogHeader>
            <div className="flex flex-col items-center gap-3 py-2">
              <div className="w-full max-w-[284px] rounded-2xl border bg-white p-3 shadow-sm">
                <QRCodeSVG
                  value={`its://unlock?code=${selectedVehicle.vehicleCode}`}
                  size={250}
                  className="h-auto w-full"
                  level="M"
                  includeMargin
                />
              </div>
              <div className="w-full break-all rounded-md bg-muted px-3 py-2 text-center font-mono text-xs">
                its://unlock?code={selectedVehicle.vehicleCode}
              </div>
              <p className="text-xs text-muted-foreground text-center">
                Mở mục Quét mã QR trong app để dùng mã này.
              </p>
            </div>
          </DialogContent>
        </Dialog>
      )}
    </PageContainer>
  );
}

