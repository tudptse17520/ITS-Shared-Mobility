/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint-disable react-hooks/exhaustive-deps */
/* eslint-disable @typescript-eslint/no-unused-vars */
import axiosClient from "@/lib/axios-client";
import { VehicleType, VehicleTypeRequest } from "@/features/vehicle-types/types/vehicle-type.type";

const BASE_PATH = "/vehicle-types";

export const getVehicleTypes = async (): Promise<VehicleType[]> => {
  const [vehicleTypesRes, zonesRes, occupancyRes] = await Promise.all([
    axiosClient.get<VehicleType[]>(BASE_PATH),
    axiosClient.get<any[]>("/station-zones").catch(() => ({ data: [] })),
    axiosClient.get<any>("/reports/occupancy").catch(() => ({ data: { details: [] } }))
  ]);

  const vehicleTypes = vehicleTypesRes.data;
  const zones = zonesRes.data || [];
  const occupancyDetails = occupancyRes.data?.details || [];

  // Tạo map tra cứu số chỗ đang bị chiếm dụng theo tên tầng
  const occupiedMap = new Map<string, number>();
  occupancyDetails.forEach((d: any) => {
    if (d.zoneName && d.occupiedDocks !== undefined) {
      occupiedMap.set(d.zoneName, d.occupiedDocks);
    }
  });

  return vehicleTypes.map(vt => {
    const activeSessionsCount = zones
      .filter(f => f.vehicleTypeId === vt.id)
      .reduce((sum, f) => sum + (occupiedMap.get(f.zoneName) || 0), 0);
    
    return {
      ...vt,
      activeSessionsCount
    };
  });
};

export const createVehicleType = async (data: VehicleTypeRequest): Promise<VehicleType> => {
  const response = await axiosClient.post<VehicleType>(BASE_PATH, data);
  return response.data;
};

export const updateVehicleType = async (
  id: number,
  data: VehicleTypeRequest
): Promise<VehicleType> => {
  const response = await axiosClient.put<VehicleType>(`${BASE_PATH}/${id}`, data);
  return response.data;
};

export const deactivateVehicleType = async (id: number): Promise<VehicleType> => {
  const response = await axiosClient.patch<VehicleType>(`${BASE_PATH}/${id}/deactivate`);
  return response.data;
};

export const deleteVehicleType = async (id: number): Promise<void> => {
  await axiosClient.delete(`${BASE_PATH}/${id}`);
};
