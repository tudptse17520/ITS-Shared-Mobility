import axiosClient from '@/lib/axios-client';

import type { StationListResponse, StationActionResponse } from '@/types/station.type';
import type { StationCreateForm, StationUpdateForm } from '@/features/stations/schemas/station-form.schema';

const STATION_API_URL = `/stations`;

export const getStations = async (keyword?: string, status?: string): Promise<StationListResponse> => {
  const params: Record<string, string> = {};
  if (keyword) params.keyword = keyword;
  if (status && status !== 'ALL') params.status = status;

  const response = await axiosClient.get<StationListResponse>(STATION_API_URL, { params });
  return response.data;
};

export const createStation = async (data: StationCreateForm): Promise<StationActionResponse> => {
  const response = await axiosClient.post<StationActionResponse>(STATION_API_URL, data);
  return response.data;
};

export const updateStation = async (id: number, data: StationUpdateForm): Promise<StationActionResponse> => {
  const response = await axiosClient.put<StationActionResponse>(`${STATION_API_URL}/${id}`, data);
  return response.data;
};

export const updateStationStatus = async (id: number, status: string): Promise<StationActionResponse> => {
  const response = await axiosClient.patch<StationActionResponse>(`${STATION_API_URL}/${id}/status`, { status });
  return response.data;
};

export const deleteStation = async (id: number): Promise<StationActionResponse> => {
  const response = await axiosClient.delete<StationActionResponse>(`${STATION_API_URL}/${id}`);
  return response.data;
};
