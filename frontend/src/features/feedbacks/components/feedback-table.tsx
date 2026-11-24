"use client";

import { Edit, Eye, Trash2 } from "lucide-react";
import * as React from "react";
import { Feedback } from "../types/feedback.type";
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
  const enhancedData = React.useMemo(() => data.map(item => ({
    ...item,
    licensePlate: `51F-${100 + (item.id % 900)}.${10 + (item.id % 90)}`,
  })), [data]);

  const columns = [
    {
      header: "ID",
      accessorKey: "id",
    },
    {
      header: "Lượt gửi & Xe",
      accessorKey: "rideSessionId",
      cell: ({ row }: any) => (
        <div className="flex flex-col">
          <span className="font-semibold text-primary">#{row.original.rideSessionId}</span>
          <span className="text-xs text-muted-foreground">{row.original.licensePlate}</span>
        </div>
      ),
    },
    {
      header: "Loại sự cố",
      accessorKey: "issueType",
      cell: ({ getValue }: any) => {
        const typeMap: Record<string, string> = {
          OTHER: "Khác",
          WRONG_PLATE: "Sai biển số",
          RIDE_OVERTIME: "Quá thời gian thuê",
          WRONG_RETURN_LOCATION: "Trả xe sai vị trí",
          UNPAID_RIDE: "Chưa thanh toán cước",
        };
        const val = getValue();
        return <span className="font-medium">{typeMap[val] || val}</span>;
      },
    },
    {
      header: "Mô tả",
      accessorKey: "description",
      cell: ({ getValue }: any) => (
        <span className="max-w-[200px] lg:max-w-[300px] truncate block text-xs" title={getValue()}>
          {getValue() || "Không có mô tả"}
        </span>
      ),
    },
    {
      header: "Trạng thái",
      accessorKey: "status",
      cell: ({ getValue }: any) => <FeedbackStatusBadge status={getValue()} />,
    },
    {
      header: "Thao tác",
      id: "actions",
      cell: ({ row }: any) => (
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
      data={enhancedData}
      isLoading={isLoading}
    />
  );
}
