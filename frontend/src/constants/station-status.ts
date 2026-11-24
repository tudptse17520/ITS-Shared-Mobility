// ---------------------------------------------
// Station Status Constants
// Trạng thái trạm xe
// ---------------------------------------------

export enum StationStatus {
  ACTIVE = "ACTIVE",
  MAINTENANCE = "MAINTENANCE",
  INACTIVE = "INACTIVE",
}

export const STATION_STATUS_LABELS: Record<StationStatus, string> = {
  [StationStatus.ACTIVE]: "Hoạt động",
  [StationStatus.MAINTENANCE]: "Bảo trì",
  [StationStatus.INACTIVE]: "Ngưng hoạt động",
};
