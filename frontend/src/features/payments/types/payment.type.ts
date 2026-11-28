// ---------------------------------------------
// Payment Types
// Kiểu dữ liệu cho chức năng thanh toán
// ---------------------------------------------

/**
 * Response DTO từ Backend khi lấy chi tiết / item trong danh sách giao dịch.
 * Map 1:1 với PaymentResponseDTO.java
 */
export interface Payment {
  id: number;
  rideSessionId: number | null;
  bookingId: number | null;
  amount: number;
  paymentMethod: string;
  feeType: string;
  paymentTime: string;
  status: string;
  payment_url?: string;
}

/**
 * Wrapper response từ Backend khi GET danh sách giao dịch.
 * Map 1:1 với PaymentListResponseDTO.java
 */
export interface PaymentListResponse {
  totalItems: number;
  message: string;
  data: Payment[];
}
