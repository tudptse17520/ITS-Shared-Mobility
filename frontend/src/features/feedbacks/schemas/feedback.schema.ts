import * as z from "zod";

export const feedbackFormSchema = z.object({
  rideSessionId: z.number().min(1, { message: "Vui lòng chọn chuyến đi" }),
  issueType: z.enum(["OTHER", "WRONG_PLATE", "RIDE_OVERTIME", "WRONG_RETURN_LOCATION", "UNPAID_RIDE"]),
  description: z.string().min(1, { message: "Vui lòng nhập mô tả sự cố" }),
  status: z.enum(["REPORTED", "PROCESSING", "RESOLVED"]),
});

export type FeedbackFormValues = z.infer<typeof feedbackFormSchema>;
