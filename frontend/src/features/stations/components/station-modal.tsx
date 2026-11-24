/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint-disable react-hooks/exhaustive-deps */
/* eslint-disable @typescript-eslint/no-unused-vars */
"use client";

import React, { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { cn } from '@/lib/utils';
import { FormContainer, FormHeader, FormFields, FormActions } from '@/components/common/form-container';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { StationStatus } from '@/constants/station-status';
import { stationCreateSchema, stationUpdateSchema, type StationCreateForm, type StationUpdateForm } from '../schemas/station-form.schema';
import type { StationResponse } from '@/types/station.type';
import { XIcon as X } from "@phosphor-icons/react/ssr";
import { useCreateStation, useUpdateStation } from '../hooks/use-stations';
import { Portal } from '@/components/common/portal';

interface StationModalProps {
  open: boolean;
  onClose: () => void;
  station?: StationResponse | null;
}

export function StationModal({ open, onClose, station }: StationModalProps) {
  const isEditMode = !!station;
  
  const { mutate: createStation, isPending: isCreating } = useCreateStation();
  const { mutate: updateStation, isPending: isUpdating } = useUpdateStation();
  const isPending = isCreating || isUpdating;

  const form = useForm<StationCreateForm | StationUpdateForm>({
    resolver: zodResolver(isEditMode ? stationUpdateSchema as any : stationCreateSchema as any),
    defaultValues: {
      stationName: '',
      address: '',
      numberOfZones: 1,
      status: StationStatus.ACTIVE,
    },
  });

  useEffect(() => {
    if (open) {
      if (station) {
        form.reset({
          stationName: station.stationName,
          address: station.address,
          numberOfZones: station.numberOfZones,
          status: station.status,
        });
      } else {
        form.reset({
          stationName: '',
          address: '',
          numberOfZones: 1,
          status: StationStatus.ACTIVE,
        });
      }
    }
  }, [open, station, form]);

  const onSubmit = (data: StationCreateForm | StationUpdateForm) => {
    if (isEditMode && station) {
      updateStation(
        { id: station.id, data: data as StationUpdateForm },
        { onSuccess: onClose }
      );
    } else {
      createStation(
        data as StationCreateForm,
        { onSuccess: onClose }
      );
    }
  };

  if (!open) return null;

  return (
    <Portal>
      <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
        {/* Backdrop */}
        <div 
          className="fixed inset-0 bg-black/40 backdrop-blur-sm animate-backdrop" 
          onClick={onClose} 
          aria-hidden="true" 
        />

        {/* Modal Content */}
        <div className="relative z-50 w-full max-w-md rounded-xl border border-border/60 bg-background p-6 shadow-xl animate-scale-in">
          <button
            onClick={onClose}
            className="absolute right-4 top-4 rounded-lg p-1 text-muted-foreground/60 hover:text-foreground hover:bg-muted transition-colors duration-200"
            type="button"
          >
            <X className="h-4 w-4" />
            <span className="sr-only">Đóng</span>
          </button>

          <FormContainer onSubmit={form.handleSubmit(onSubmit)}>
            <FormHeader 
              title={isEditMode ? 'Cập nhật Trạm Xe' : 'Thêm Trạm Xe Mới'} 
              description={isEditMode ? 'Thay đổi thông tin của trạm xe hiện tại.' : 'Nhập thông tin cho trạm xe mới.'}
            />

            <FormFields>
              <div className="space-y-1.5">
                <Label htmlFor="stationName">
                  Tên Trạm Xe <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="stationName"
                  {...form.register('stationName')}
                  className={cn(form.formState.errors.stationName && 'border-destructive focus-visible:ring-destructive/30')}
                  placeholder="Nhập tên trạm xe"
                  disabled={isPending}
                />
                {form.formState.errors.stationName && (
                  <p className="text-xs text-destructive">{form.formState.errors.stationName.message}</p>
                )}
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="numberOfZones">
                  Số Khu Dock <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="numberOfZones"
                  type="number"
                  {...form.register('numberOfZones', { valueAsNumber: true })}
                  className={cn(form.formState.errors.numberOfZones && 'border-destructive focus-visible:ring-destructive/30')}
                  placeholder="Nhập số khu dock"
                  disabled={isPending}
                />
                {form.formState.errors.numberOfZones && (
                  <p className="text-xs text-destructive">{form.formState.errors.numberOfZones.message}</p>
                )}
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="address">
                  Địa Chỉ <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="address"
                  {...form.register('address')}
                  className={cn(form.formState.errors.address && 'border-destructive focus-visible:ring-destructive/30')}
                  placeholder="Nhập địa chỉ"
                  disabled={isPending}
                />
                {form.formState.errors.address && (
                  <p className="text-xs text-destructive">{form.formState.errors.address.message}</p>
                )}
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="status">
                  Trạng Thái <span className="text-destructive">*</span>
                </Label>
                <select
                  id="status"
                  {...form.register('status')}
                  className="flex h-11 w-full rounded-lg border border-input bg-background px-3.5 py-2 text-sm shadow-sm transition-colors duration-200 hover:border-ring/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/30 focus-visible:border-ring disabled:cursor-not-allowed disabled:opacity-50"
                  disabled={isPending}
                >
                  <option value="ACTIVE">Đang hoạt động</option>
                  <option value="MAINTENANCE">Bảo trì</option>
                  <option value="INACTIVE">Ngừng hoạt động</option>
                </select>
                {form.formState.errors.status && (
                  <p className="text-xs text-destructive">{form.formState.errors.status.message}</p>
                )}
              </div>
            </FormFields>

            <FormActions>
              <Button type="button" variant="outline" onClick={onClose} disabled={isPending}>
                Hủy
              </Button>
              <Button type="submit" disabled={isPending}>
                {isPending ? 'Đang lưu...' : (isEditMode ? 'Cập nhật' : 'Thêm mới')}
              </Button>
            </FormActions>
          </FormContainer>
        </div>
      </div>
    </Portal>
  );
}
