"use client";

import { useEffect, useState, useSyncExternalStore, type ComponentType } from "react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import {
  ShieldWarningIcon as ShieldAlert,
  UserPlusIcon as UserPlus,
  GearSixIcon as Settings,
  UserMinusIcon as UserMinus,
  PencilSimpleIcon as Edit,
  MapPinIcon as MapPin,
  PlusIcon as Plus,
} from "@phosphor-icons/react/ssr";
import { useActivityStore } from "@/stores/activity.store";

const iconMap: Record<string, ComponentType<{ className?: string }>> = {
  UserPlus,
  UserMinus,
  Settings,
  ShieldAlert,
  Edit,
  MapPin,
  MapPinPlus: Plus,
};

function getTimeAgo(timestamp: number, now: number): string {
  if (!now || now <= timestamp) return "Vừa xong";
  const seconds = Math.floor((now - timestamp) / 1000);
  if (seconds < 60) return "Vừa xong";
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes} phút trước`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} giờ trước`;
  const days = Math.floor(hours / 24);
  return `${days} ngày trước`;
}

const emptySubscribe = () => () => {};

export function AdminSystemActivity() {
  const activities = useActivityStore((state) => state.activities);
  const [currentTime, setCurrentTime] = useState<number>(0);
  const isMounted = useSyncExternalStore(
    emptySubscribe,
    () => true,
    () => false
  );

  useEffect(() => {
    const initialTick = setTimeout(() => setCurrentTime(Date.now()), 0);
    const interval = setInterval(() => setCurrentTime(Date.now()), 60000);
    return () => {
      clearTimeout(initialTick);
      clearInterval(interval);
    };
  }, []);

  if (!isMounted) {
    return null;
  }

  return (
    <Card className="min-w-0">
      <CardHeader>
        <CardTitle>Hoạt động gần đây</CardTitle>
        <CardDescription>Thao tác trên trình duyệt này</CardDescription>
      </CardHeader>
      <CardContent>
        {activities.length === 0 && (
          <p className="py-4 text-sm text-muted-foreground">Chưa có thao tác nào.</p>
        )}
        <div className="space-y-4 max-h-64 overflow-y-auto pr-2 custom-scrollbar">
          {activities.map((activity) => {
            const Icon = iconMap[activity.icon] || Settings;
            return (
              <div key={activity.id} className="flex items-start gap-4">
                <div className={`flex h-10 w-10 mt-0.5 shrink-0 items-center justify-center rounded-full ${activity.bg}`}>
                  <Icon className={`h-5 w-5 ${activity.color}`} />
                </div>
                <div className="flex-1 space-y-1">
                  <p className="text-sm font-medium leading-none">{activity.title}</p>
                  <p className="text-xs text-muted-foreground">
                    {getTimeAgo(activity.timestamp, currentTime)}
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      </CardContent>
    </Card>
  );
}
