"use client";

import React, { useState } from 'react';
import { PageContainer } from '@/components/common/page-container';
import { PageHeader } from '@/components/common/page-header';
import { Toolbar } from '@/components/common/toolbar';
import { Button } from '@/components/ui/button';
import { MagnifyingGlassIcon as Search } from "@phosphor-icons/react/ssr";
import { StationFilter } from '@/features/stations/components/station-filter';
import { StationTable } from '@/features/stations/components/station-table';
import { StationModal } from '@/features/stations/components/station-modal';
import { StationStats } from '@/features/stations/components/station-stats';
import { useStations } from '@/features/stations/hooks/use-stations';
import { TableSkeleton } from '@/components/common/table-skeleton';
import { EmptyState } from '@/components/common/empty-state';
import type { StationResponse } from '@/types/station.type';

export default function StationsPage() {
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState('ALL');
  
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedStation, setSelectedStation] = useState<StationResponse | null>(null);

  const { data, isLoading } = useStations(keyword, status);

  const handleAddStation = () => {
    setSelectedStation(null);
    setIsModalOpen(true);
  };

  const handleEditStation = (station: StationResponse) => {
    setSelectedStation(station);
    setIsModalOpen(true);
  };

  return (
    <PageContainer>
      <PageHeader 
        title="Quản Lý Trạm Xe Công Cộng" 
        description="Quản lý vị trí trạm đỗ xe chia sẻ, sức chứa dock và theo dõi trạng thái vận hành của từng trạm." 
        actions={
          <Button onClick={handleAddStation}>
            Thêm trạm xe
          </Button>
        } 
      />
      
      <StationStats />

      <Toolbar>
        <StationFilter 
          keyword={keyword}
          status={status}
          onKeywordChange={setKeyword}
          onStatusChange={setStatus}
        />
      </Toolbar>
      
      {isLoading ? (
        <TableSkeleton rows={5} />
      ) : (!data?.data || data.data.length === 0) ? (
        <EmptyState 
          title="Không tìm thấy trạm xe" 
          description="Thử thay đổi bộ lọc hoặc từ khóa tìm kiếm trạm xe." 
          icon={Search}
        />
      ) : (
        <StationTable 
          stations={data.data} 
          onEdit={handleEditStation}
        />
      )}

      <StationModal 
        open={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        station={selectedStation}
      />
    </PageContainer>
  );
}
