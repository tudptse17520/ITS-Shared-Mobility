export type IssueType = "OTHER" | "WRONG_PLATE" | "RIDE_OVERTIME" | "WRONG_RETURN_LOCATION" | "UNPAID_RIDE";

export type FeedbackStatus = "REPORTED" | "PROCESSING" | "RESOLVED";

export interface Feedback {
  id: number;
  rideSessionId: number;
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
