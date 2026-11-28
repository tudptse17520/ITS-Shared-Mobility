import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getAllBookings,
  cancelBooking,
  updateBookingStatus,
  deleteBooking,
} from "@/services/booking.service";
import toast from "react-hot-toast";

const BOOKING_KEYS = {
  all: ["bookings"] as const,
  lists: () => [...BOOKING_KEYS.all, "list"] as const,
};

export const useAllBookings = () => {
  return useQuery({
    queryKey: BOOKING_KEYS.lists(),
    queryFn: () => getAllBookings(),
    refetchInterval: 5000,
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
      toast.success("Hủy lượt giữ xe thành công!");
    },
    onError: (error: unknown) => {
      const msg = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
      toast.error(msg || "Hủy lượt giữ xe thất bại");
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
      toast.success("Cập nhật trạng thái lượt giữ xe thành công!");
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
      toast.success("Xóa lượt giữ xe thành công!");
    },
    onError: (error: unknown) => {
      const msg = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
      toast.error(msg || "Xóa lượt giữ xe thất bại");
    }
  });
};
