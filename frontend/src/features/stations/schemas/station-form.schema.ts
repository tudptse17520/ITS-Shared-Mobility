import { z } from 'zod';
import { StationStatus } from '@/constants/station-status';

export const stationCreateSchema = z.object({
  stationName: z.string().min(3, { message: 'Tên trạm xe phải có ít nhất 3 ký tự' }).max(100, { message: 'Tên trạm xe không vượt quá 100 ký tự' }),
  address: z.string().min(1, { message: 'Địa chỉ không được để trống' }).max(255, { message: 'Địa chỉ không vượt quá 255 ký tự' }),
  numberOfZones: z.coerce.number().min(1, { message: 'Số khu vực phải lớn hơn hoặc bằng 1' }),
  status: z.nativeEnum(StationStatus),
});

export const stationUpdateSchema = z.object({
  stationName: z.string().min(3, { message: 'Tên trạm xe phải có ít nhất 3 ký tự' }).max(100, { message: 'Tên trạm xe không vượt quá 100 ký tự' }),
  address: z.string().min(1, { message: 'Địa chỉ không được để trống' }).max(255, { message: 'Địa chỉ không vượt quá 255 ký tự' }),
  numberOfZones: z.coerce.number().min(1, { message: 'Số khu vực phải lớn hơn hoặc bằng 1' }),
  status: z.nativeEnum(StationStatus),
});

export interface StationCreateForm {
  stationName: string;
  address: string;
  numberOfZones: number;
  status: StationStatus;
}

export interface StationUpdateForm {
  stationName: string;
  address: string;
  numberOfZones: number;
  status: StationStatus;
}
