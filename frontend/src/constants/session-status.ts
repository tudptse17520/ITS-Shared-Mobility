// ---------------------------------------------
// Session Status Constants
// Trạng thái chuyến đi
// ---------------------------------------------

export enum SessionStatus {
  IN_PROGRESS = "IN_PROGRESS",
  AWAITING_PAYMENT = "AWAITING_PAYMENT",
  COMPLETED = "COMPLETED",
}

export const SESSION_STATUS_LABELS: Record<SessionStatus, string> = {
  [SessionStatus.IN_PROGRESS]: "Đang đi",
  [SessionStatus.AWAITING_PAYMENT]: "Chờ thanh toán",
  [SessionStatus.COMPLETED]: "Hoàn tất",
};
