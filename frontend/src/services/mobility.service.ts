import axiosClient from "@/lib/axios-client";

export interface StationItem {
  id: number;
  stationName: string;
  address: string;
  latitude: number;
  longitude: number;
  totalDocks: number;
  availableVehicles: number;
  status: string;
}

export interface FleetVehicleItem {
  id: number;
  vehicleCode: string;
  plate: string;
  brand: string;
  color: string;
  vehicleTypeId: number | null;
  vehicleTypeName: string | null;
  currentStationId: number | null;
  currentStationName: string | null;
  latitude: number;
  longitude: number;
  batteryLevel: number;
  lockStatus: string;
  fleetStatus: string;
  distanceMeters: number | null;
  unlockFee: number;
  hourlyRate: number;
}

export interface TelemetryLogItem {
  id: number;
  vehicleId: number;
  vehicleCode: string;
  sessionId: number | null;
  latitude: number;
  longitude: number;
  batteryLevel: number;
  speedKmh: number;
  lockStatus: string;
  recordedAt: string;
}

export interface RideSessionItem {
  sessionId: number;
  sessionCode: string;
  userId: number;
  username: string;
  vehicleId: number;
  vehicleCode: string;
  plate: string;
  vehicleBrand: string;
  vehicleTypeName: string;
  startLatitude: number;
  startLongitude: number;
  currentLatitude: number;
  currentLongitude: number;
  endLatitude: number | null;
  endLongitude: number | null;
  distanceKm: number;
  currentBatteryLevel: number;
  lockStatus: string;
  startTime: string;
  endTime: string | null;
  durationMinutes: number;
  estimatedFee: number;
  totalFee: number | null;
  status: string;
  paid: boolean;
}

export const mobilityService = {
  async getStations(): Promise<StationItem[]> {
    const res = await axiosClient.get<StationItem[]>("/mobility/stations");
    return res.data;
  },

  async getAllVehicles(): Promise<FleetVehicleItem[]> {
    const res = await axiosClient.get<FleetVehicleItem[]>("/mobility/vehicles");
    return res.data;
  },

  async reserveVehicle(vehicleId: number, userId?: number) {
    const res = await axiosClient.post("/mobility/reserve", {
      vehicleId,
      userId,
      holdMinutes: 15,
    });
    return res.data;
  },

  async cancelReservation(vehicleId: number) {
    const res = await axiosClient.post(`/mobility/vehicles/${vehicleId}/cancel-reserve`);
    return res.data;
  },

  async startRide(vehicleId: number, userId?: number): Promise<RideSessionItem> {
    const res = await axiosClient.post<RideSessionItem>("/mobility/rides/start", {
      vehicleId,
      userId,
    });
    return res.data;
  },

  async endRide(sessionId: number, endLatitude?: number, endLongitude?: number): Promise<RideSessionItem> {
    const res = await axiosClient.post<RideSessionItem>(`/mobility/rides/${sessionId}/end`, {
      endLatitude,
      endLongitude,
    });
    return res.data;
  },

  async simulateRideStep(sessionId: number): Promise<RideSessionItem> {
    const res = await axiosClient.post<RideSessionItem>(`/iot/simulate-step/${sessionId}`);
    return res.data;
  },

  async controlVehicle(
    vehicleId: number,
    command: "UNLOCK" | "LOCK" | "RECHARGE_100" | "SET_BATTERY" | "MAINTENANCE" | "SET_AVAILABLE" | "MOVE_GPS",
    payload?: { batteryLevel?: number; latitude?: number; longitude?: number }
  ): Promise<FleetVehicleItem> {
    const res = await axiosClient.post<FleetVehicleItem>(`/iot/vehicles/${vehicleId}/control`, {
      command,
      ...payload,
    });
    return res.data;
  },

  async getTelemetryHistory(vehicleId: number): Promise<TelemetryLogItem[]> {
    const res = await axiosClient.get<TelemetryLogItem[]>(`/iot/telemetry/${vehicleId}`);
    return res.data;
  },

  async getActiveRide(userId?: number): Promise<RideSessionItem | null> {
    const res = await axiosClient.get<RideSessionItem | null>("/mobility/rides/active", {
      params: userId ? { userId } : undefined,
    });
    return res.data || null;
  },

  async getRideHistory(userId?: number): Promise<RideSessionItem[]> {
    const res = await axiosClient.get<RideSessionItem[]>("/mobility/rides/history", {
      params: userId ? { userId } : undefined,
    });
    return Array.isArray(res.data) ? res.data : [];
  },
};

