import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  createBooking,
  getUserBookings,
  getAllBookings,
  cancelBooking,
  updateBookingStatus,
  deleteBooking,
} from "@/services/booking.service";
import type { BookingRequest } from "../types/booking.type";
import toast from "react-hot-toast";

export const BOOKING_KEYS = {
  all: ["bookings"] as const,
  lists: () => [...BOOKING_KEYS.all, "list"] as const,
  userLists: (userId: number) => [...BOOKING_KEYS.all, "user", userId] as const,
};

export const useUserBookings = (userId: number | undefined) => {
  const effectiveId = userId && userId > 0 ? userId : 0;
  return useQuery({
    queryKey: BOOKING_KEYS.userLists(effectiveId),
    queryFn: () => getUserBookings(effectiveId),
    enabled: true,
  });
};

export const useAllBookings = () => {
  return useQuery({
    queryKey: BOOKING_KEYS.lists(),
    queryFn: () => getAllBookings(),
    refetchInterval: 5000,
  });
};

export const useCreateBooking = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: BookingRequest) => createBooking(request),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: BOOKING_KEYS.all,
      });
      queryClient.invalidateQueries({
        queryKey: ["docks"],
      });
      toast.success("Đặt chỗ thành công!");
    },
    onError: (error: unknown) => {
      const msg = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
      toast.error(msg || "Đặt chỗ thất bại");
    }
  });
};

export const useCancelBooking = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (bookingId: number) => cancelBooking(bookingId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: BOOKING_KEYS.all,
      });
      queryClient.invalidateQueries({
        queryKey: ["docks"],
      });
      toast.success("Hủy đặt chỗ thành công!");
    },
    onError: (error: unknown) => {
      const msg = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
      toast.error(msg || "Hủy đặt chỗ thất bại");
    }
  });
};

export const useUpdateBookingStatus = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ bookingId, status }: { bookingId: number; status: string }) =>
      updateBookingStatus(bookingId, status),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: BOOKING_KEYS.all,
      });
      queryClient.invalidateQueries({
        queryKey: ["docks"],
      });
      toast.success("Cập nhật trạng thái đặt chỗ thành công!");
    },
    onError: (error: unknown) => {
      const msg = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
      toast.error(msg || "Cập nhật trạng thái thất bại");
    }
  });
};

export const useDeleteBooking = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (bookingId: number) => deleteBooking(bookingId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: BOOKING_KEYS.all,
      });
      queryClient.invalidateQueries({
        queryKey: ["docks"],
      });
      toast.success("Xóa đặt chỗ thành công!");
    },
    onError: (error: unknown) => {
      const msg = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
      toast.error(msg || "Xóa đặt chỗ thất bại");
    }
  });
};
