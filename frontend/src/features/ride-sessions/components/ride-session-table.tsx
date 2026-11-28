"use client";

import { useMemo, useState } from "react";
import type { ColumnDef } from "@tanstack/react-table";
import { format, parseISO } from "date-fns";
import { CheckCircleIcon, TrashIcon } from "@phosphor-icons/react/ssr";
import { DataTable } from "@/components/common/data-table";
import { ConfirmDialog } from "@/components/common/confirm-dialog";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { SessionStatus, SESSION_STATUS_LABELS } from "@/constants/session-status";
import type { RideSession } from "../types/ride-session.type";
import { useDeleteSession, useUpdateSessionStatus } from "../hooks/use-ride-sessions";

interface RideSessionTableProps {
  data: RideSession[];
  isLoading?: boolean;
}

function formatDateTime(value?: string | null): string {
  if (!value) return "—";
  try {
    return format(parseISO(value), "dd/MM/yyyy HH:mm:ss");
  } catch {
    return value;
  }
}

export function RideSessionStatusBadge({ status }: { status: SessionStatus | string }) {
  const label = SESSION_STATUS_LABELS[status as SessionStatus] || status;
  if (status === SessionStatus.COMPLETED) {
    return (
      <Badge variant="outline" className="bg-emerald-500/15 text-emerald-600 border-emerald-500/30">
        {label}
      </Badge>
    );
  }
  if (status === SessionStatus.AWAITING_PAYMENT) {
    return (
      <Badge variant="outline" className="bg-amber-500/15 text-amber-600 border-amber-500/30">
        {label}
      </Badge>
    );
  }
  return (
    <Badge variant="outline" className="bg-blue-500/15 text-blue-600 border-blue-500/30">
      {label}
    </Badge>
  );
}

export function RideSessionTable({ data, isLoading = false }: RideSessionTableProps) {
  const [deleteTarget, setDeleteTarget] = useState<RideSession | null>(null);
  const updateStatusMutation = useUpdateSessionStatus();
  const deleteMutation = useDeleteSession();

  const columns = useMemo<ColumnDef<RideSession>[]>(
    () => [
      {
        accessorKey: "sessionCode",
        header: "Mã chuyến",
        cell: ({ row }) => (
          <div className="flex flex-col">
            <span className="font-mono text-xs font-semibold text-primary">
              {row.original.sessionCode || `#${row.original.id}`}
            </span>
            <span className="text-[11px] text-muted-foreground">ID #{row.original.id}</span>
          </div>
        ),
      },
      {
        id: "customer",
        header: "Khách thuê",
        cell: ({ row }) => (
          <div className="flex flex-col">
            <span className="font-medium text-foreground">
              {row.original.customerName || "Khách thuê"}
            </span>
            <span className="text-xs text-muted-foreground">
              {row.original.customerPhone || "—"}
            </span>
          </div>
        ),
      },
      {
        id: "vehicle",
        header: "Phương tiện",
        cell: ({ row }) => (
          <div className="flex flex-col">
            <span className="font-medium text-foreground">
              {row.original.vehicleCode
                ? `${row.original.vehicleCode} · ${row.original.plate}`
                : row.original.plate}
            </span>
            <span className="text-xs text-muted-foreground">
              {row.original.vehicleTypeName || "Xe chia sẻ"}
            </span>
          </div>
        ),
      },
      {
        accessorKey: "distanceKm",
        header: "Quãng đường",
        cell: ({ row }) => (
          <span className="tabular-nums text-sm font-medium">
            {(row.original.distanceKm ?? 0).toFixed(2)} km
          </span>
        ),
      },
      {
        id: "timeline",
        header: "Thời gian chuyến đi",
        cell: ({ row }) => (
          <div className="flex flex-col gap-0.5 text-xs">
            <span className="text-emerald-600 dark:text-emerald-400">
              Mở khóa: <span className="font-medium">{formatDateTime(row.original.startedAt)}</span>
            </span>
            <span className="text-muted-foreground">
              Kết thúc:{" "}
              <span className="font-medium text-foreground">
                {row.original.endedAt ? formatDateTime(row.original.endedAt) : "Đang di chuyển"}
              </span>
            </span>
          </div>
        ),
      },
      {
        accessorKey: "totalFee",
        header: "Cước phí",
        cell: ({ row }) => (
          <span className="tabular-nums font-semibold text-foreground">
            {row.original.totalFee != null
              ? `${Math.round(row.original.totalFee).toLocaleString("vi-VN")} đ`
              : "Đang tính"}
          </span>
        ),
      },
      {
        accessorKey: "status",
        header: "Trạng thái",
        cell: ({ row }) => <RideSessionStatusBadge status={row.original.status} />,
      },
      {
        id: "actions",
        header: () => <div className="text-right">Thao tác</div>,
        cell: ({ row }) => {
          const item = row.original;
          return (
            <div className="flex items-center justify-end gap-1.5">
              {item.status === SessionStatus.AWAITING_PAYMENT && (
                <Button
                  variant="outline"
                  size="sm"
                  disabled={updateStatusMutation.isPending}
                  onClick={() =>
                    updateStatusMutation.mutate({
                      id: item.id,
                      status: SessionStatus.COMPLETED,
                    })
                  }
                  className="h-8 px-2.5 text-xs"
                  title="Xác nhận hoàn tất thu tiền mặt tại quầy"
                >
                  <CheckCircleIcon className="mr-1 h-4 w-4" />
                  Xác nhận thu
                </Button>
              )}
              <Button
                variant="ghost"
                size="icon"
                onClick={() => setDeleteTarget(item)}
                className="h-8 w-8 text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
                title="Xóa chuyến đi"
              >
                <TrashIcon className="h-4 w-4" />
              </Button>
            </div>
          );
        },
      },
    ],
    [updateStatusMutation],
  );

  return (
    <>
      <div className="w-full overflow-x-auto rounded-md border border-border bg-card">
        <DataTable columns={columns} data={data} isLoading={isLoading} />
      </div>

      <ConfirmDialog
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => {
          if (deleteTarget) {
            deleteMutation.mutate(deleteTarget.id, {
              onSuccess: () => setDeleteTarget(null),
            });
          }
        }}
        title="Xác nhận xóa chuyến đi"
        description={`Bạn có chắc chắn muốn xóa chuyến đi ${deleteTarget?.sessionCode ?? ""} không? Hành động này không thể hoàn tác.`}
        confirmText="Xóa chuyến đi"
        cancelText="Đóng"
        variant="danger"
        isLoading={deleteMutation.isPending}
      />
    </>
  );
}

