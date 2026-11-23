import { StationStatus } from '@/constants/station-status';

export interface StationResponse {
  id: number;
  stationName: string;
  address: string;
  numberOfZones: number;
  totalAvailableDocks: number;
  status: StationStatus;
}

export interface StationListResponse {
  data: StationResponse[];
  message: string;
}

export interface StationActionResponse {
  id: number;
  message: string;
}
