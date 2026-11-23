import React from 'react';
import { StatusBadge, type BadgeVariant } from '@/components/common/status-badge';
import type { StationResponse } from '@/types/station.type';

interface StationStatusBadgeProps {
  status: StationResponse['status'];
  className?: string;
}

const statusConfig: Record<StationResponse['status'], { label: string; variant: BadgeVariant }> = {
  ACTIVE: {
    label: 'Đang hoạt động',
    variant: 'success',
  },
  MAINTENANCE: {
    label: 'Bảo trì',
    variant: 'warning',
  },
  INACTIVE: {
    label: 'Ngừng hoạt động',
    variant: 'danger',
  },
};

export function StationStatusBadge({ status, className }: StationStatusBadgeProps) {
  const config = statusConfig[status];

  if (!config) return null;

  return (
    <StatusBadge variant={config.variant} className={className}>
      {config.label}
    </StatusBadge>
  );
}
