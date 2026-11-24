import { useQuery } from "@tanstack/react-query";
import type { SessionFilterParams } from "@/features/ride-sessions/types/ride-session.type";
import { sessionService } from "@/services/ride-session.service";

export function useSessionsList(filters?: SessionFilterParams) {
  return useQuery({
    queryKey: ["sessions", "list", filters],
    queryFn: () => sessionService.getSessions(filters),
  });
}
