"use client";

import { PencilSimpleIcon as Edit, EyeIcon as Eye, TrashIcon as Trash2 } from "@phosphor-icons/react/ssr";
import type { ColumnDef } from "@tanstack/react-table";
import { Feedback, type FeedbackStatus, type IssueType } from "../types/feedback.type";
import { DataTable } from "@/components/common/data-table";
import { Button } from "@/components/ui/button";
import { FeedbackStatusBadge } from "./feedback-status-badge";

interface FeedbackTableProps {
  data: Feedback[];
  isLoading?: boolean;
  onView: (feedback: Feedback) => void;
  onEdit: (feedback: Feedback) => void;
  onDelete?: (feedback: Feedback) => void;
}

export function FeedbackTable({ data, isLoading, onView, onEdit, onDelete }: FeedbackTableProps) {
  const columns: ColumnDef<Feedback>[] = [
    {
      header: "ID",
      accessorKey: "id",
      cell: ({ row }) => <span className="font-mono text-xs">#{row.original.id}</span>,
    },
    {
      header: "Chuyến đi & Phương tiện",
      accessorKey: "rideSessionId",
      cell: ({ row }) => {
        const subInfo = [
          row.original.vehicleCode,
          row.original.licensePlate,
          row.original.customerName,
        ]
          .filter(Boolean)
          .join(" · ");
        return (
          <div className="flex flex-col">
            <span className="font-mono text-xs font-semibold text-primary">
              {row.original.sessionCode || `#${row.original.rideSessionId}`}
            </span>
            <span className="text-xs text-muted-foreground">
              {subInfo || `Phiên #${row.original.rideSessionId}`}
            </span>
          </div>
        );
      },
    },
    {
      header: "Loại sự cố",
      accessorKey: "issueType",
      cell: ({ getValue }) => {
        const typeMap: Record<string, string> = {
          OTHER: "Khác",
          WRONG_PLATE: "Sai biển số",
          RIDE_OVERTIME: "Quá thời gian thuê",
          WRONG_RETURN_LOCATION: "Trả xe sai vị trí",
          UNPAID_RIDE: "Chưa thanh toán cước",
        };
        const val = getValue<IssueType>();
        return <span className="font-medium">{typeMap[val] || val}</span>;
      },
    },
    {
      header: "Mô tả",
      accessorKey: "description",
      cell: ({ getValue }) => {
        const description = getValue<string | undefined>();
        return (
          <span className="max-w-[200px] lg:max-w-[300px] truncate block text-xs" title={description}>
            {description || "Không có mô tả"}
          </span>
        );
      },
    },
    {
      header: "Trạng thái",
      accessorKey: "status",
      cell: ({ getValue }) => <FeedbackStatusBadge status={getValue<FeedbackStatus>()} />,
    },
    {
      header: "Thao tác",
      id: "actions",
      cell: ({ row }) => (
        <div className="flex gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => onView(row.original)}
          >
            <Eye className="w-4 h-4 mr-2" />
            Chi tiết
          </Button>
          <Button
            variant="default"
            size="sm"
            onClick={() => onEdit(row.original)}
          >
            <Edit className="w-4 h-4 mr-2" />
            Xử lý
          </Button>
          {onDelete && (
            <Button
              variant="ghost"
              size="icon"
              onClick={() => onDelete(row.original)}
              className="text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
              title="Xóa sự cố"
            >
              <Trash2 className="w-4 h-4" />
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <DataTable
      columns={columns}
      data={data}
      isLoading={isLoading}
    />
  );
}
