"use client";

import { useReducedMotion } from "framer-motion";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { useUsers } from "@/features/users/hooks/use-users";

export function AdminDashboardCharts() {
  const { data } = useUsers();
  const reducedMotion = useReducedMotion();
  const active = data?.data?.filter((user) => user.status === "ACTIVE") ?? [];
  const roles = [
    { name: "Quản trị viên", count: active.filter((user) => user.role === "ADMIN").length },
    { name: "Khách thuê", count: active.filter((user) => user.role === "USER").length },
  ];

  return (
    <Card className="min-w-0 xl:col-span-2">
      <CardHeader>
        <CardTitle>Tài khoản theo vai trò</CardTitle>
        <CardDescription>Chỉ tính tài khoản đang hoạt động</CardDescription>
      </CardHeader>
      <CardContent>
        <div className="h-56 w-full min-w-0">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={roles} margin={{ top: 12, right: 6, left: 0, bottom: 0 }}>
              <CartesianGrid stroke="var(--border)" strokeDasharray="4 4" vertical={false} />
              <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fill: "var(--muted-foreground)", fontSize: 12 }} />
              <YAxis width={30} axisLine={false} tickLine={false} allowDecimals={false} tick={{ fill: "var(--muted-foreground)", fontSize: 12 }} />
              <Tooltip cursor={{ fill: "var(--muted)", opacity: 0.6 }}
                contentStyle={{ borderRadius: 12, borderColor: "var(--border)", background: "var(--popover)", color: "var(--foreground)" }}
                itemStyle={{ color: "var(--foreground)" }} />
              <Bar dataKey="count" name="Tài khoản" fill="var(--chart-1)" radius={[7, 7, 0, 0]} maxBarSize={54} animationDuration={420} isAnimationActive={!reducedMotion} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </CardContent>
    </Card>
  );
}
