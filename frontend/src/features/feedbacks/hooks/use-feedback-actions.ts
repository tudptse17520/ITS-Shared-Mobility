import { useMutation, useQueryClient } from "@tanstack/react-query";
import toast from "react-hot-toast";
import { feedbackService } from "@/services/feedback.service";
import { FeedbackRequest } from "../types/feedback.type";
import { FEEDBACK_QUERY_KEYS } from "./use-feedbacks";
import { getApiErrorMessage } from "@/lib/api-error";

export function useFeedbackActions() {
  const queryClient = useQueryClient();

  const createFeedbackMutation = useMutation({
    mutationFn: (request: FeedbackRequest) => feedbackService.createFeedback(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: FEEDBACK_QUERY_KEYS.all });
    },
  });

  const updateFeedbackMutation = useMutation({
    mutationFn: ({ id, request }: { id: number; request: FeedbackRequest }) =>
      feedbackService.updateFeedback(id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: FEEDBACK_QUERY_KEYS.all });
    },
  });

  const deleteFeedbackMutation = useMutation({
    mutationFn: (id: number) => feedbackService.deleteFeedback(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: FEEDBACK_QUERY_KEYS.all });
      toast.success("Xóa sự cố thành công!");
    },
    onError: (error: unknown) => {
      toast.error(getApiErrorMessage(error, "Xóa sự cố thất bại"));
    },
  });

  return {
    createFeedbackMutation,
    updateFeedbackMutation,
    deleteFeedbackMutation,
  };
}
