"use client";

import React, { useState } from 'react';
import { type ColumnDef } from '@tanstack/react-table';
import { DataTable } from '@/components/common/data-table';
import { ConfirmDialog } from '@/components/common/confirm-dialog';
import { StationStatusBadge } from './station-status-badge';
import type { StationResponse } from '@/types/station.type';
import { Button } from '@/components/ui/button';
import { Edit2, Ban, Trash2 } from 'lucide-react';
import { useUpdateStationStatus, useDeleteStation } from '../hooks/use-stations';

interface StationTableProps {
  stations: StationResponse[];
  onEdit: (station: StationResponse) => void;
  isLoading?: boolean;
}

export function StationTable({ stations, onEdit, isLoading }: StationTableProps) {
  const [deactivateConfirmOpen, setDeactivateConfirmOpen] = useState(false);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [selectedStation, setSelectedStation] = useState<StationResponse | null>(null);

  const { mutate: updateStationStatus, isPending: isUpdating } = useUpdateStationStatus();
  const { mutate: deleteStation, isPending: isDeleting } = useDeleteStation();

  const handleDeactivateClick = (station: StationResponse) => {
    setSelectedStation(station);
    setDeactivateConfirmOpen(true);
  };

  const handleConfirmDeactivate = () => {
    if (!selectedStation) return;
    
    updateStationStatus({
      id: selectedStation.id,
      status: 'INACTIVE',
    }, {
      onSuccess: () => {
        setDeactivateConfirmOpen(false);
        setSelectedStation(null);
      }
    });
  };

  const handleDeleteClick = (station: StationResponse) => {
    setSelectedStation(station);
    setDeleteConfirmOpen(true);
  };

  const handleConfirmDelete = () => {
    if (!selectedStation) return;

    deleteStation(selectedStation.id, {
      onSuccess: () => {
        setDeleteConfirmOpen(false);
        setSelectedStation(null);
      }
    });
  };

  const columns: ColumnDef<StationResponse>[] = [
    {
      accessorKey: 'id',
      header: 'ID',
    },
    {
      accessorKey: 'stationName',
      header: 'Tên Trạm Xe',
    },
    {
      accessorKey: 'address',
      header: 'Địa Chỉ',
      cell: ({ row }) => (
        <div 
          className="truncate max-w-[200px] text-muted-foreground hover:text-foreground transition-colors cursor-default" 
          title={row.original.address}
        >
          {row.original.address}
        </div>
      )
    },
    {
      accessorKey: 'totalAvailableDocks',
      header: 'Sức Chứa / Dock Trống',
      cell: ({ row }) => {
        const docks = row.original.totalAvailableDocks;
        return (
          <div className="font-medium">
            {docks != null ? (
              <span className="text-emerald-600 dark:text-emerald-400">{docks} trống</span>
            ) : (
              <span className="text-muted-foreground/50 italic">Đang cập nhật</span>
            )}
          </div>
        );
      }
    },
    {
      accessorKey: 'status',
      header: 'Trạng Thái',
      cell: ({ row }) => (
        <StationStatusBadge status={row.original.status} />
      ),
    },
    {
      id: 'actions',
      header: 'Thao Tác',
      cell: ({ row }) => (
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="icon-sm"
            onClick={() => onEdit(row.original)}
            title="Sửa"
          >
            <Edit2 />
          </Button>
          {row.original.status !== 'INACTIVE' && (
            <Button
              variant="ghost"
              size="icon-sm"
              onClick={() => handleDeactivateClick(row.original)}
              title="Ngừng hoạt động"
              className="text-muted-foreground hover:text-amber-600 hover:bg-amber-500/20"
            >
              <Ban className="h-4 w-4" />
            </Button>
          )}
          <Button
            variant="ghost"
            size="icon-sm"
            onClick={() => handleDeleteClick(row.original)}
            title="Xóa trạm xe"
            className="text-muted-foreground hover:text-destructive hover:bg-destructive/20"
          >
            <Trash2 className="h-4 w-4" />
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="w-full overflow-x-auto">
      <DataTable
        columns={columns}
        data={stations}
        isLoading={isLoading}
        className="min-w-[800px]"
      />
      
      <ConfirmDialog
        open={deactivateConfirmOpen}
        onClose={() => setDeactivateConfirmOpen(false)}
        onConfirm={handleConfirmDeactivate}
        title="Ngừng hoạt động trạm xe"
        description={`Bạn có chắc chắn muốn chuyển trạng thái trạm xe ${selectedStation?.stationName} sang Ngừng hoạt động?`}
        confirmText="Ngừng hoạt động"
        cancelText="Hủy"
        variant="danger"
        isLoading={isUpdating}
      />

      <ConfirmDialog
        open={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Xóa trạm xe"
        description={`Bạn có chắc chắn muốn xóa vĩnh viễn trạm xe ${selectedStation?.stationName}? Hành động này không thể hoàn tác.`}
        confirmText="Xóa"
        cancelText="Hủy"
        variant="danger"
        isLoading={isDeleting}
      />
    </div>
  );
}
