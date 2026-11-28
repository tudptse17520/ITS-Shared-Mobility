export interface Payment {
  id: number;
  rideSessionId: number | null;
  bookingId: number | null;
  amount: number;
  paymentMethod: "Cash" | "Vnpay" | "Momo" | "Credit_Card";
  feeType: "RIDE_FEE" | "BOOKING_DEPOSIT" | "HISTORICAL_ADJUSTMENT";
  paymentTime: string | null;
  status: "PENDING" | "SUCCESS" | "FAILED";
  paymentUrl: string | null;
}

export interface PaymentListResponse {
  totalItems: number;
  message: string;
  data: Payment[];
}
