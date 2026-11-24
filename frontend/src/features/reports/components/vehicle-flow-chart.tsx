import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { useVehicleFlowReport } from "../hooks/use-report";
import { LoadingSpinner } from "@/components/common/loading-spinner";
import { EmptyState } from "@/components/common/empty-state";
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid, Legend } from "recharts";
import { format, parseISO } from "date-fns";
import { vi } from "date-fns/locale";
import { Activity } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { ReportFilter, ReportDetail } from "../types/report.type";

interface VehicleFlowChartProps {
  filter?: ReportFilter;
}

export function VehicleFlowChart({ filter }: VehicleFlowChartProps) {
  const { data, isLoading, isError } = useVehicleFlowReport(filter);

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Lưu lượng chuyến đi</CardTitle>
          <CardDescription>Đang tải dữ liệu...</CardDescription>
        </CardHeader>
        <CardContent className="h-[300px] flex items-center justify-center">
          <LoadingSpinner />
        </CardContent>
      </Card>
    );
  }

  if (isError || !data) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Lưu lượng xe</CardTitle>
        </CardHeader>
        <CardContent className="h-[300px]">
          <EmptyState 
             icon={Activity}
             title="Lỗi tải dữ liệu"
             description="Không thể tải thống kê lưu lượng xe lúc này."
             action={
               <Button variant="outline" onClick={() => window.location.reload()}>
                 Thử lại
               </Button>
             }
          />
        </CardContent>
      </Card>
    );
  }

  // Assuming details has { date, ridesStarted, ridesEnded }
  const chartData = data.details?.length > 0 ? data.details.map((d: ReportDetail) => {
    let name = d.name || d.date;
    if (name && name.includes("-")) {
      try {
        name = format(parseISO(name), "dd MMM", { locale: vi });
      } catch (e) {
        // ignore
      }
    }
    return {
      name: name || "Chưa xác định",
      ridesStarted: d.ridesStarted ?? 0,
      ridesEnded: d.ridesEnded ?? 0,
    };
  }) : [
    { name: "Tổng quan", ridesStarted: data.totalRidesStarted, ridesEnded: data.totalRidesEnded }
  ];

  return (
    <Card>
      <CardHeader>
        <CardTitle>Lưu lượng chuyến đi</CardTitle>
        <CardDescription>
          Bắt đầu: <span className="font-bold text-green-600">{data.totalRidesStarted}</span> |
          Kết thúc: <span className="font-bold text-blue-600">{data.totalRidesEnded}</span>
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="h-[300px] w-full mt-4">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={chartData} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
              <defs>
                <linearGradient id="colorRidesStarted" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#16a34a" stopOpacity={0.8}/>
                  <stop offset="95%" stopColor="#16a34a" stopOpacity={0}/>
                </linearGradient>
                <linearGradient id="colorRidesEnded" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#ef4444" stopOpacity={0.8}/>
                  <stop offset="95%" stopColor="#ef4444" stopOpacity={0}/>
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="name" fontSize={12} tickLine={false} axisLine={false} />
              <YAxis fontSize={12} tickLine={false} axisLine={false} />
              <Tooltip cursor={{ stroke: 'var(--muted)', strokeWidth: 1 }} />
              <Legend verticalAlign="top" height={36}/>
              <Area type="monotone" dataKey="ridesStarted" name="Chuyến bắt đầu" stroke="#16a34a" fillOpacity={0.5} fill="url(#colorRidesStarted)" />
              <Area type="monotone" dataKey="ridesEnded" name="Chuyến kết thúc" stroke="#ef4444" fillOpacity={0.5} fill="url(#colorRidesEnded)" />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </CardContent>
    </Card>
  );
}
