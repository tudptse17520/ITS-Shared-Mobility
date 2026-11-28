export type IssueType = "OTHER" | "WRONG_PLATE" | "RIDE_OVERTIME" | "WRONG_RETURN_LOCATION" | "UNPAID_RIDE";

export type FeedbackStatus = "REPORTED" | "PROCESSING" | "RESOLVED";

export interface Feedback {
  id: number;
  rideSessionId: number;
  sessionCode?: string | null;
  vehicleCode?: string | null;
  licensePlate?: string | null;
  customerName?: string | null;
  issueType: IssueType;
  description: string;
  status: FeedbackStatus;
}

export interface FeedbackRequest {
  rideSessionId: number;
  issueType: IssueType;
  description: string;
  status: FeedbackStatus;
}
