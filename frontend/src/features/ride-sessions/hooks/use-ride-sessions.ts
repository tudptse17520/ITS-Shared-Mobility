import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import type { SessionFilterParams } from "@/features/ride-sessions/types/ride-session.type";
import { sessionService } from "@/services/ride-session.service";

export function useSessionsList(filters?: SessionFilterParams) {
  return useQuery({
    queryKey: ["sessions", "list", filters],
    queryFn: () => sessionService.getSessions(filters),
    refetchInterval: 5000,
  });
}

export function useUpdateSessionStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) =>
      sessionService.updateSessionStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sessions"] });
      queryClient.invalidateQueries({ queryKey: ["reports"] });
      queryClient.invalidateQueries({ queryKey: ["fleet-vehicles"] });
      toast.success("Cập nhật trạng thái chuyến đi thành công!");
    },
    onError: (error: unknown) => {
      const msg = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
      toast.error(msg || "Cập nhật trạng thái chuyến đi thất bại");
    },
  });
}

export function useDeleteSession() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => sessionService.deleteSession(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sessions"] });
      queryClient.invalidateQueries({ queryKey: ["reports"] });
      toast.success("Đã xóa bản ghi chuyến đi!");
    },
    onError: (error: unknown) => {
      const msg = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
      toast.error(msg || "Xóa chuyến đi thất bại");
    },
  });
}
