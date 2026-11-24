import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getStations,
  getStationDetail,
  createStation,
  updateStation,
  updateStationStatus,
  deleteStation,
} from '@/services/station.service';
import type { StationCreateForm, StationUpdateForm } from '../schemas/station-form.schema';
import toast from "react-hot-toast";
import { useAuthStore } from "@/stores/auth.store";
import { useActivityStore } from "@/stores/activity.store";

export const useStations = (keyword?: string, status?: string) => {
  return useQuery({
    queryKey: ['stations', keyword, status],
    queryFn: () => getStations(keyword, status),
    refetchInterval: 5000,
  });
};

export const useStationDetail = (id: number) => {
  return useQuery({
    queryKey: ['station', id],
    queryFn: () => getStationDetail(id),
    enabled: !!id,
  });
};

export const useCreateStation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: StationCreateForm) => createStation(data),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['stations'] });
      toast.success(`Thêm trạm xe "${variables.stationName}" thành công!`);

      const { user } = useAuthStore.getState();
      const actorName = user?.fullName || user?.username || "Admin";
      
      useActivityStore.getState().addActivity({
        type: "create_station",
        title: `${actorName} đã thêm trạm xe mới ${variables.stationName}`,
        icon: "MapPinPlus",
        color: "text-blue-500",
        bg: "bg-blue-500/10",
      });
    },
    onError: (error: unknown) => {
      const msg = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
      toast.error(msg || "Đã có lỗi xảy ra khi thêm trạm xe");
    }
  });
};

export const useUpdateStation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: StationUpdateForm }) => updateStation(id, data),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['stations'] });
      toast.success(`Cập nhật trạm xe "${variables.data.stationName}" thành công!`);

      const { user } = useAuthStore.getState();
      const actorName = user?.fullName || user?.username || "Admin";
      
      useActivityStore.getState().addActivity({
        type: "update_station",
        title: `${actorName} đã chỉnh sửa trạm xe ${variables.data.stationName}`,
        icon: "Edit",
        color: "text-amber-500",
        bg: "bg-amber-500/10",
      });
    },
    onError: (error: unknown) => {
      const msg = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
      toast.error(msg || "Đã có lỗi xảy ra khi cập nhật trạm xe");
    }
  });
};

export const useUpdateStationStatus = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) => updateStationStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['stations'] });
      toast.success("Cập nhật trạng thái trạm xe thành công!");
    },
    onError: (error: unknown) => {
      const msg = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
      toast.error(msg || "Đã có lỗi xảy ra");
    }
  });
};

export const useDeleteStation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => deleteStation(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['stations'] });
      toast.success("Xóa trạm xe thành công!");
    },
    onError: (error: unknown) => {
      const msg = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
      toast.error(msg || "Không thể xóa trạm xe");
    }
  });
};
