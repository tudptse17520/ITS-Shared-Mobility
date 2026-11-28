"use client";

import { useMemo, type ComponentType } from "react";
import { format, parseISO } from "date-fns";
import {
  BicycleIcon,
  CalendarCheckIcon,
  CheckCircleIcon,
  ClockIcon,
  GearSixIcon as Settings,
  MapPinIcon as MapPin,
  PencilSimpleIcon as Edit,
  PlusIcon as Plus,
  ShieldWarningIcon as ShieldAlert,
  UserMinusIcon as UserMinus,
  UserPlusIcon as UserPlus,
  WarningCircleIcon,
} from "@phosphor-icons/react/ssr";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { useActivityStore } from "@/stores/activity.store";
import { useSessionsList } from "@/features/ride-sessions/hooks/use-ride-sessions";
import { useAllBookings } from "@/features/bookings/hooks/use-bookings";
import { useFeedbacks } from "@/features/feedbacks/hooks/use-feedbacks";
import { SessionStatus } from "@/constants/session-status";

const localIconMap: Record<string, ComponentType<{ className?: string }>> = {
  UserPlus,
  UserMinus,
  Settings,
  ShieldAlert,
  Edit,
  MapPin,
  MapPinPlus: Plus,
};

const ISSUE_LABELS: Record<string, string> = {
  OTHER: "Sự cố khác",
  WRONG_PLATE: "Sai biển số",
  RIDE_OVERTIME: "Quá thời gian thuê",
  WRONG_RETURN_LOCATION: "Trả xe sai vị trí",
  UNPAID_RIDE: "Chưa thanh toán cước",
};

interface UnifiedFeedItem {
  id: string;
  title: string;
  subtitle: string;
  sortTime: number;
  Icon: ComponentType<{ className?: string }>;
  color: string;
  bg: string;
}

function parseTimestampMs(iso?: string | null): number {
  if (!iso) return 0;
  const parsed = Date.parse(iso);
  return Number.isNaN(parsed) ? 0 : parsed;
}

function formatShortTime(iso?: string | null): string {
  if (!iso) return "Vừa ghi nhận";
  try {
    return format(parseISO(iso), "dd/MM HH:mm:ss");
  } catch {
    return iso;
  }
}

export function AdminSystemActivity() {
  const localActivities = useActivityStore((state) => state.activities);
  const { data: sessionsData } = useSessionsList();
  const { data: bookingsData = [] } = useAllBookings();
  const { data: feedbacksData } = useFeedbacks(1, 10);

  const feedItems = useMemo<UnifiedFeedItem[]>(() => {
    const items: UnifiedFeedItem[] = [];

    for (const session of (sessionsData?.data ?? []).slice(0, 8)) {
      const refTime = session.endedAt || session.startedAt;
      const feeText =
        session.totalFee != null ? ` · ${Math.round(session.totalFee).toLocaleString("vi-VN")} đ` : "";
      const who = session.customerName || "Khách thuê";
      const bike = session.vehicleCode || session.plate;

      if (session.status === SessionStatus.COMPLETED) {
        items.push({
          id: `ride-done-${session.id}`,
          title: `${who} hoàn tất chuyến ${session.sessionCode}`,
          subtitle: `${bike}${feeText} · ${formatShortTime(refTime)}`,
          sortTime: parseTimestampMs(refTime) || session.id * 1000,
          Icon: CheckCircleIcon,
          color: "text-emerald-600 dark:text-emerald-400",
          bg: "bg-emerald-500/15",
        });
      } else if (session.status === SessionStatus.AWAITING_PAYMENT) {
        items.push({
          id: `ride-wait-${session.id}`,
          title: `${who} kết thúc chuyến ${session.sessionCode} (Chờ thanh toán)`,
          subtitle: `${bike}${feeText} · ${formatShortTime(refTime)}`,
          sortTime: parseTimestampMs(refTime) || session.id * 1000,
          Icon: ClockIcon,
          color: "text-amber-600 dark:text-amber-400",
          bg: "bg-amber-500/15",
        });
      } else {
        items.push({
          id: `ride-live-${session.id}`,
          title: `${who} đang di chuyển trên xe ${bike}`,
          subtitle: `${session.sessionCode} · Mở khóa ${formatShortTime(session.startedAt)}`,
          sortTime: parseTimestampMs(session.startedAt) || session.id * 1000,
          Icon: BicycleIcon,
          color: "text-blue-600 dark:text-blue-400",
          bg: "bg-blue-500/15",
        });
      }
    }

    for (const booking of bookingsData.slice(0, 6)) {
      const refTime = booking.createdAt || booking.plannedStartAt;
      const who = booking.customerName || "Khách thuê";
      const bike = booking.vehicleCode || booking.licensePlate || `#${booking.bookingId}`;
      const statusText =
        booking.status === "CONFIRMED"
          ? "đặt giữ 15 phút"
          : booking.status === "CANCELLED"
            ? "đã hủy đặt giữ"
            : booking.status === "USED"
              ? "đã nhận xe từ lượt giữ chỗ"
              : `đặt giữ (${booking.status})`;
      items.push({
        id: `booking-${booking.bookingId}-${booking.status}`,
        title: `${who} ${statusText} xe ${bike}`,
        subtitle: `${booking.stationName || booking.stationDockName || "Trạm ITS"} · ${formatShortTime(refTime)}`,
        sortTime: parseTimestampMs(refTime) || booking.bookingId * 1000,
        Icon: CalendarCheckIcon,
        color: booking.status === "CANCELLED" ? "text-rose-500" : "text-primary",
        bg: booking.status === "CANCELLED" ? "bg-rose-500/15" : "bg-primary/15",
      });
    }

    const sessionById = new Map((sessionsData?.data ?? []).map((s) => [s.id, s]));
    for (const fb of (feedbacksData?.data ?? []).slice(0, 5)) {
      const issueLabel = ISSUE_LABELS[fb.issueType] || fb.issueType;
      const relatedSession = sessionById.get(fb.rideSessionId);
      const refTime = relatedSession?.endedAt || relatedSession?.startedAt;
      items.push({
        id: `feedback-${fb.id}`,
        title: `Báo cáo sự cố: ${issueLabel}`,
        subtitle: `${fb.sessionCode || `Phiên #${fb.rideSessionId}`} · ${fb.description || "Từ ứng dụng di động"}`,
        sortTime: (parseTimestampMs(refTime) || fb.id * 1000) + fb.id,
        Icon: WarningCircleIcon,
        color: fb.status === "RESOLVED" ? "text-emerald-500" : "text-orange-500",
        bg: fb.status === "RESOLVED" ? "bg-emerald-500/15" : "bg-orange-500/15",
      });
    }

    for (const act of localActivities.slice(0, 5)) {
      const Icon = localIconMap[act.icon] || Settings;
      items.push({
        id: `local-${act.id}`,
        title: act.title,
        subtitle: format(act.timestamp, "dd/MM HH:mm:ss"),
        sortTime: act.timestamp,
        Icon,
        color: act.color,
        bg: act.bg,
      });
    }

    return items.sort((a, b) => b.sortTime - a.sortTime).slice(0, 12);
  }, [bookingsData, feedbacksData?.data, localActivities, sessionsData?.data]);

  return (
    <Card className="min-w-0">
      <CardHeader>
        <CardTitle>Hoạt động gần đây</CardTitle>
        <CardDescription>Đồng bộ trực tiếp từ ứng dụng Mobile & hệ thống</CardDescription>
      </CardHeader>
      <CardContent>
        {feedItems.length === 0 ? (
          <p className="py-4 text-sm text-muted-foreground">Chưa có hoạt động nào được ghi nhận.</p>
        ) : (
          <div className="custom-scrollbar max-h-72 space-y-3.5 overflow-y-auto pr-1.5">
            {feedItems.map((item) => {
              const Icon = item.Icon;
              return (
                <div key={item.id} className="flex items-start gap-3">
                  <div
                    className={`mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-full ${item.bg}`}
                  >
                    <Icon className={`h-4 w-4 ${item.color}`} />
                  </div>
                  <div className="min-w-0 flex-1 space-y-0.5">
                    <p className="truncate text-sm font-medium leading-snug text-foreground" title={item.title}>
                      {item.title}
                    </p>
                    <p className="truncate text-xs text-muted-foreground" title={item.subtitle}>
                      {item.subtitle}
                    </p>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
