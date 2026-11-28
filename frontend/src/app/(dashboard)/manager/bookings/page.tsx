"use client";

import { useState, useMemo } from "react";
import { MagnifyingGlassIcon } from "@phosphor-icons/react/ssr";
import { PageContainer } from "@/components/common/page-container";
import { PageHeader } from "@/components/common/page-header";
import { Toolbar } from "@/components/common/toolbar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { BookingTable, type BookingContextData } from "@/features/bookings/components/booking-table";
import { BookingFilter } from "@/features/bookings/components/booking-filter";
import { useAllBookings } from "@/features/bookings/hooks/use-bookings";
import { useSessionsList } from "@/features/ride-sessions/hooks/use-ride-sessions";
import { RideSessionTable } from "@/features/ride-sessions/components/ride-session-table";
import { SessionStatus } from "@/constants/session-status";

type ActiveTab = "sessions" | "bookings";

export default function ManagerBookingsPage() {
  const [activeTab, setActiveTab] = useState<ActiveTab>("sessions");

  // Ride Sessions state
  const { data: sessionResponse, isLoading: isLoadingSessions, error: sessionsError } = useSessionsList();
  const [sessionKeyword, setSessionKeyword] = useState("");
  const [sessionStatusFilter, setSessionStatusFilter] = useState<string>("ALL");

  // Bookings (15m holds) state
  const { data: rawBookings = [], isLoading: isLoadingBookings, error: bookingsError } = useAllBookings();
  const [bookingKeyword, setBookingKeyword] = useState("");
  const [bookingStatusFilter, setBookingStatusFilter] = useState<string | undefined>(undefined);

  const filteredSessions = useMemo(() => {
    let list = sessionResponse?.data ?? [];
    if (sessionStatusFilter !== "ALL") {
      list = list.filter((s) => s.status === sessionStatusFilter);
    }
    if (sessionKeyword.trim()) {
      const kw = sessionKeyword.trim().toLowerCase();
      list = list.filter(
        (s) =>
          s.sessionCode?.toLowerCase().includes(kw) ||
          s.plate?.toLowerCase().includes(kw) ||
          s.vehicleCode?.toLowerCase().includes(kw) ||
          s.customerName?.toLowerCase().includes(kw) ||
          s.customerPhone?.toLowerCase().includes(kw) ||
          String(s.id).includes(kw),
      );
    }
    return list;
  }, [sessionResponse?.data, sessionKeyword, sessionStatusFilter]);

  const bookings = useMemo<BookingContextData[]>(() => {
    let enriched: BookingContextData[] = rawBookings.map((b) => ({
      ...b,
      customerName: b.customerName || "Khách thuê",
      customerPhone: b.customerPhone || "—",
      licensePlate: b.licensePlate || "Chưa cập nhật",
      vehicleTypeName: b.vehicleTypeName || "Xe chia sẻ",
    }));

    if (bookingStatusFilter) {
      enriched = enriched.filter((b) => b.status === bookingStatusFilter);
    }

    if (bookingKeyword.trim()) {
      const lowerKw = bookingKeyword.trim().toLowerCase();
      enriched = enriched.filter(
        (b) =>
          b.bookingId.toString().includes(lowerKw) ||
          b.customerName?.toLowerCase().includes(lowerKw) ||
          b.vehicleCode?.toLowerCase().includes(lowerKw) ||
          b.licensePlate?.toLowerCase().includes(lowerKw) ||
          b.stationName?.toLowerCase().includes(lowerKw) ||
          b.customerPhone?.toLowerCase().includes(lowerKw),
      );
    }

    return enriched;
  }, [rawBookings, bookingKeyword, bookingStatusFilter]);

  const totalSessionsCount = sessionResponse?.data?.length ?? 0;
  const activeReservationsCount = rawBookings.filter((b) => b.status === "CONFIRMED" || b.status === "PENDING").length;

  return (
    <PageContainer>
      <PageHeader
        title="Quản lý Đặt giữ & Chuyến đi"
        description="Đồng bộ trực tiếp lịch sử mở khóa QR, thanh toán chuyến đi và giữ chỗ 15 phút từ ứng dụng di động."
      />

      <div className="flex flex-wrap items-center gap-2 border-b pb-3">
        <Button
          type="button"
          variant={activeTab === "sessions" ? "default" : "outline"}
          onClick={() => setActiveTab("sessions")}
          className="gap-2"
        >
          Chuyến đi thực tế
          <span className="rounded-full bg-background/20 px-2 py-0.5 text-xs tabular-nums">
            {totalSessionsCount}
          </span>
        </Button>
        <Button
          type="button"
          variant={activeTab === "bookings" ? "default" : "outline"}
          onClick={() => setActiveTab("bookings")}
          className="gap-2"
        >
          Đặt giữ xe 15 phút
          <span className="rounded-full bg-background/20 px-2 py-0.5 text-xs tabular-nums">
            {rawBookings.length}
            {activeReservationsCount > 0 ? ` (${activeReservationsCount} đang giữ)` : ""}
          </span>
        </Button>
      </div>

      {activeTab === "sessions" ? (
        <>
          <Toolbar>
            <div className="flex w-full flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div className="relative w-full sm:max-w-sm">
                <MagnifyingGlassIcon className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  value={sessionKeyword}
                  onChange={(e) => setSessionKeyword(e.target.value)}
                  placeholder="Tìm mã chuyến (RIDE-...), xe, biển số, khách thuê..."
                  className="pl-9"
                />
              </div>
              <div className="flex flex-wrap items-center gap-1.5">
                {[
                  { key: "ALL", label: "Tất cả" },
                  { key: SessionStatus.IN_PROGRESS, label: "Đang đi" },
                  { key: SessionStatus.AWAITING_PAYMENT, label: "Chờ thanh toán" },
                  { key: SessionStatus.COMPLETED, label: "Hoàn tất" },
                ].map((opt) => (
                  <Button
                    key={opt.key}
                    type="button"
                    size="sm"
                    variant={sessionStatusFilter === opt.key ? "default" : "outline"}
                    onClick={() => setSessionStatusFilter(opt.key)}
                  >
                    {opt.label}
                  </Button>
                ))}
              </div>
            </div>
          </Toolbar>

          {sessionsError ? (
            <div className="mt-4 flex items-center justify-center py-8 text-sm text-destructive">
              Đã xảy ra lỗi khi tải danh sách chuyến đi.
            </div>
          ) : (
            <div className="mt-4">
              <RideSessionTable data={filteredSessions} isLoading={isLoadingSessions} />
            </div>
          )}
        </>
      ) : (
        <>
          <Toolbar>
            <BookingFilter
              keyword={bookingKeyword}
              onKeywordChange={setBookingKeyword}
              status={bookingStatusFilter}
              onStatusChange={setBookingStatusFilter}
            />
          </Toolbar>

          {bookingsError ? (
            <div className="mt-4 flex items-center justify-center py-8 text-sm text-destructive">
              Đã xảy ra lỗi khi tải danh sách đặt giữ xe.
            </div>
          ) : (
            <div className="mt-4">
              <BookingTable data={bookings} isLoading={isLoadingBookings} isManagerView />
            </div>
          )}
        </>
      )}
    </PageContainer>
  );
}
