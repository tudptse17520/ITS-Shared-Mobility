import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { AxiosError } from "axios";
import { ApiErrorResponse } from "@/types/api";
import {
  getVehicleTypes,
  createVehicleType,
  updateVehicleType,
  deactivateVehicleType,
  deleteVehicleType,
} from "@/services/vehicle-type.service";
import {
  VehicleType,
  VehicleTypeRequest,
} from "@/features/vehicle-types/types/vehicle-type.type";

import toast from "react-hot-toast";

export const VEHICLE_TYPES_QUERY_KEY = ["vehicle-types"];

export const useVehicleTypes = () => {
  return useQuery({
    queryKey: VEHICLE_TYPES_QUERY_KEY,
    queryFn: getVehicleTypes,
    refetchInterval: 5000,
  });
};

export const useCreateVehicleType = () => {
  const queryClient = useQueryClient();

  return useMutation<
    VehicleType,
    AxiosError<ApiErrorResponse>,
    VehicleTypeRequest
  >({
    mutationFn: (data) => createVehicleType(data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: VEHICLE_TYPES_QUERY_KEY });
      toast.success(`Thêm loại xe "${variables.typeName}" thành công!`);
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || "Đã có lỗi xảy ra khi thêm loại xe");
    }
  });
};

export const useUpdateVehicleType = () => {
  const queryClient = useQueryClient();

  return useMutation<
    VehicleType,
    AxiosError<ApiErrorResponse>,
    { id: number; data: VehicleTypeRequest }
  >({
    mutationFn: ({ id, data }) => updateVehicleType(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: VEHICLE_TYPES_QUERY_KEY });
      toast.success(`Cập nhật loại xe "${variables.data.typeName}" thành công!`);
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || "Đã có lỗi xảy ra khi cập nhật loại xe");
    }
  });
};

export const useDeactivateVehicleType = () => {
  const queryClient = useQueryClient();

  return useMutation<VehicleType, AxiosError<ApiErrorResponse>, { id: number; typeName: string }>({
    mutationFn: ({ id }) => deactivateVehicleType(id),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: VEHICLE_TYPES_QUERY_KEY });
      toast.success(`Ngừng hoạt động loại xe "${variables.typeName}" thành công!`);
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || "Đã có lỗi xảy ra");
    }
  });
};

export const useDeleteVehicleType = () => {
  const queryClient = useQueryClient();

  return useMutation<void, AxiosError<ApiErrorResponse>, { id: number; typeName: string }>({
    mutationFn: ({ id }) => deleteVehicleType(id),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: VEHICLE_TYPES_QUERY_KEY });
      toast.success(`Xóa loại xe "${variables.typeName}" thành công!`);
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || "Không thể xóa loại xe");
    }
  });
};
