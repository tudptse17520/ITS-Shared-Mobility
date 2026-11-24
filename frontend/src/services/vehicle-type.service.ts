import axiosClient from "@/lib/axios-client";
import type { VehicleType, VehicleTypeRequest } from "@/features/vehicle-types/types/vehicle-type.type";
import type { FleetVehicleItem } from "@/services/mobility.service";

const BASE_PATH = "/vehicle-types";

export const getVehicleTypes = async (): Promise<VehicleType[]> => {
  const [vehicleTypesRes, fleetRes] = await Promise.all([
    axiosClient.get<VehicleType[]>(BASE_PATH),
    axiosClient.get<FleetVehicleItem[]>("/mobility/vehicles").catch(() => ({ data: [] as FleetVehicleItem[] })),
  ]);

  const vehicleTypes = vehicleTypesRes.data;
  const fleet = fleetRes.data || [];

  const activeByType = new Map<number, number>();
  for (const vehicle of fleet) {
    if (vehicle.vehicleTypeId != null && (vehicle.fleetStatus === "IN_USE" || vehicle.fleetStatus === "RESERVED")) {
      activeByType.set(vehicle.vehicleTypeId, (activeByType.get(vehicle.vehicleTypeId) ?? 0) + 1);
    }
  }

  return vehicleTypes.map((vt) => ({
    ...vt,
    activeSessionsCount: activeByType.get(vt.id) ?? 0,
  }));
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
