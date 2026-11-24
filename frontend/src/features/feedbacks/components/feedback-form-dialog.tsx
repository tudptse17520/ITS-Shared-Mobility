"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import toast from "react-hot-toast";
import { X } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { FormContainer, FormHeader, FormFields, FormActions } from "@/components/common/form-container";

import { Feedback } from "../types/feedback.type";
import { feedbackFormSchema, FeedbackFormValues } from "../schemas/feedback.schema";
import { useFeedbackActions } from "../hooks/use-feedback-actions";
import { useSessionsList } from "@/features/ride-sessions/hooks/use-ride-sessions";
import { Portal } from "@/components/common/portal";
import { SESSION_STATUS_LABELS } from "@/constants/session-status";

interface FeedbackFormDialogProps {
  feedback: Feedback | null;
  isOpen: boolean;
  onClose: () => void;
  readOnly?: boolean;
}

export function FeedbackFormDialog({
  feedback,
  isOpen,
  onClose,
  readOnly = false,
}: FeedbackFormDialogProps) {
  const { createFeedbackMutation, updateFeedbackMutation } = useFeedbackActions();
  const { data: sessionsResp } = useSessionsList();
  const sessions = sessionsResp?.data || [];
  const isEditing = !!feedback;

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors },
  } = useForm<FeedbackFormValues>({
    resolver: zodResolver(feedbackFormSchema),
    defaultValues: {
      rideSessionId: 0,
      issueType: "WRONG_RETURN_LOCATION",
      description: "",
      status: "REPORTED",
    },
  });

  useEffect(() => {
    if (feedback && isOpen) {
      reset({
        rideSessionId: feedback.rideSessionId,
        issueType: feedback.issueType,
        description: feedback.description,
        status: feedback.status,
      });
    } else if (!isOpen) {
      reset({
        rideSessionId: 0,
        issueType: "WRONG_RETURN_LOCATION",
        description: "",
        status: "REPORTED",
      });
    }
  }, [feedback, isOpen, reset]);

  useEffect(() => {
    if (isOpen && !feedback && sessions.length > 0) {
      const currentSessionId = watch("rideSessionId");
      if (!currentSessionId || currentSessionId === 0) {
        setValue("rideSessionId", sessions[0].id);
      }
    }
  }, [isOpen, feedback, sessions, watch, setValue]);

  const onSubmit = (values: FeedbackFormValues) => {
    if (readOnly) {
      onClose();
      return;
    }

    if (isEditing) {
      updateFeedbackMutation.mutate(
        { id: feedback.id, request: values },
        {
          onSuccess: () => {
            toast.success("Cập nhật sự cố thành công");
            onClose();
          },
          onError: (error: any) => {
            toast.error(error.response?.data?.message || "Lỗi khi cập nhật");
          },
        }
      );
    } else {
      createFeedbackMutation.mutate(values, {
        onSuccess: () => {
          toast.success("Ghi nhận sự cố thành công");
          onClose();
        },
        onError: (error: any) => {
          toast.error(error.response?.data?.message || "Lỗi khi tạo sự cố");
        },
      });
    }
  };

  const isPending = createFeedbackMutation.isPending || updateFeedbackMutation.isPending;

  if (!isOpen) return null;

  return (
    <Portal>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div 
        className="fixed inset-0 bg-black/50 transition-opacity" 
        onClick={onClose} 
        aria-hidden="true" 
      />

      <div className="relative z-50 w-full max-w-md rounded-lg bg-background p-6 shadow-lg">
        <button
          onClick={onClose}
          className="absolute right-4 top-4 text-muted-foreground hover:text-foreground"
          type="button"
        >
          <X className="h-5 w-5" />
          <span className="sr-only">Đóng</span>
        </button>

        <FormContainer onSubmit={handleSubmit(onSubmit)}>
          <FormHeader 
            title={readOnly ? "Chi tiết sự cố" : isEditing ? "Cập nhật sự cố" : "Ghi nhận sự cố mới"} 
            description={readOnly ? "Xem thông tin báo cáo sự cố." : "Điền thông tin để báo cáo hệ thống."}
          />

          <FormFields>
            <div className="space-y-2">
              <Label htmlFor="rideSessionId">Chuyến đi <span className="text-destructive">*</span></Label>
              {!isEditing && !readOnly && sessions.length > 0 ? (
                <select
                  id="rideSessionId"
                  {...register("rideSessionId", { valueAsNumber: true })}
                  disabled={isPending}
                  className={`flex h-10 w-full items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 ${errors.rideSessionId ? "border-destructive" : ""}`}
                >
                  <option value={0} disabled>-- Chọn chuyến đi --</option>
                  {sessions.map((s) => (
                    <option key={s.id} value={s.id}>
                      #{s.id} - {s.plate} ({SESSION_STATUS_LABELS[s.status]})
                    </option>
                  ))}
                </select>
              ) : (
                <Input
                  id="rideSessionId"
                  type="number"
                  placeholder="Ví dụ: 123"
                  {...register("rideSessionId", { valueAsNumber: true })}
                  className={errors.rideSessionId ? "border-destructive focus-visible:ring-destructive" : ""}
                  disabled={isPending || readOnly || isEditing}
                />
              )}
              {errors.rideSessionId && (
                <p className="text-sm text-destructive">{errors.rideSessionId.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="issueType">Loại sự cố <span className="text-destructive">*</span></Label>
              <select
                id="issueType"
                {...register("issueType")}
                disabled={isPending || readOnly || isEditing}
                className={`flex h-10 w-full items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 ${errors.issueType ? "border-destructive" : ""}`}
              >
                 <option value="OTHER">Khác</option>
                <option value="WRONG_PLATE">Sai biển số</option>
                <option value="RIDE_OVERTIME">Quá thời gian thuê</option>
                <option value="WRONG_RETURN_LOCATION">Trả xe sai vị trí</option>
                <option value="UNPAID_RIDE">Chưa thanh toán cước</option>
              </select>
              {errors.issueType && (
                <p className="text-sm text-destructive">{errors.issueType.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Mô tả chi tiết <span className="text-destructive">*</span></Label>
              <Input
                id="description"
                placeholder="Nhập mô tả sự cố..."
                {...register("description")}
                className={errors.description ? "border-destructive focus-visible:ring-destructive" : ""}
                disabled={isPending || readOnly}
              />
              {errors.description && (
                <p className="text-sm text-destructive">{errors.description.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="status">Trạng thái xử lý <span className="text-destructive">*</span></Label>
              <select
                id="status"
                {...register("status")}
                disabled={isPending || readOnly}
                className={`flex h-10 w-full items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 ${errors.status ? "border-destructive" : ""}`}
              >
                <option value="REPORTED">Đã ghi nhận</option>
                <option value="PROCESSING">Đang xử lý</option>
                <option value="RESOLVED">Đã giải quyết</option>
              </select>
              {errors.status && (
                <p className="text-sm text-destructive">{errors.status.message}</p>
              )}
            </div>
          </FormFields>

          <FormActions>
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              disabled={isPending}
            >
              {readOnly ? "Đóng" : "Hủy"}
            </Button>
            {!readOnly && (
              <Button type="submit" disabled={isPending}>
                {isPending ? "Đang xử lý..." : "Lưu thay đổi"}
              </Button>
            )}
          </FormActions>
        </FormContainer>
      </div>
    </div>
    </Portal>
  );
}

