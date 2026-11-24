// ---------------------------------------------
// Payment Service
// Gọi API thanh toán qua axiosClient
// ---------------------------------------------

import axiosClient from "@/lib/axios-client";
import { getAllBookings } from "@/services/booking.service";
import type { BookingListResponse } from "@/features/bookings/types/booking.type";
import type {
  Payment,
  PaymentListResponse,
  PaymentRequest,
  PaymentFilter,
  ManualStatusRequest,
} from "@/features/payments/types/payment.type";

const BASE_PATH = "/payments";

/**
 * Lấy danh sách giao dịch (có thể lọc theo paymentMethod, status, fromDate)
 * Backend trả về wrapper: { totalItems, message, data: [...] }
 */

export const getPayments = async (
  filter?: PaymentFilter
): Promise<Payment[]> => {
  const params: Record<string, string> = {};
  if (filter?.paymentMethod) params.paymentMethod = filter.paymentMethod;
  if (filter?.status) params.status = filter.status;
  if (filter?.fromDate) params.fromDate = filter.fromDate;

  const response = await axiosClient.get<PaymentListResponse>(BASE_PATH, {
    params,
  });
  return response.data.data;
};

/**
 * Lấy chi tiết biên lai thanh toán
 */
export const getPaymentById = async (id: number): Promise<Payment> => {
  const response = await axiosClient.get<Payment>(`${BASE_PATH}/${id}`);
  return response.data;
};

export const getRemainingDebt = async (sessionId: number): Promise<{
  session_id: number;
  total_fee: number;
  paid_fee: number;
  remaining_fee: number;
  sessionId: number;
  totalFee: number;
  paidFee: number;
  remainingFee: number;
}> => {
  const response = await axiosClient.get(`${BASE_PATH}/ride-sessions/${sessionId}/debt`);
  const d = response.data || {};
  const sid = Number(d.sessionId ?? d.session_id ?? sessionId);
  const total = Number(d.totalFee ?? d.total_fee ?? 0);
  const paid = Number(d.paidFee ?? d.paid_fee ?? 0);
  const remaining = Number(d.remainingFee ?? d.remaining_fee ?? Math.max(0, total - paid));
  return {
    session_id: sid,
    total_fee: total,
    paid_fee: paid,
    remaining_fee: remaining,
    sessionId: sid,
    totalFee: total,
    paidFee: paid,
    remainingFee: remaining,
  };
};

export { getAllBookings };

export const getBookingById = async (id: number): Promise<BookingListResponse | null> => {
  const bookings = await getAllBookings();
  return bookings.find((booking) => booking.bookingId === id) ?? null;
};

/**
 * Tạo thanh toán mới
 */
export const createPayment = async (
  data: PaymentRequest
): Promise<Payment> => {
  const payload = {
    ...data,
  };
  const response = await axiosClient.post<Payment>(BASE_PATH, payload);
  return response.data;
};

/**
 * Quản trị viên cập nhật trạng thái giao dịch từ PENDING sang SUCCESS hoặc FAILED.
 */
export const updatePaymentStatus = async (
  id: number,
  data: ManualStatusRequest
): Promise<Payment> => {
  const response = await axiosClient.patch<Payment>(
    `${BASE_PATH}/${id}/status`,
    data
  );
  return response.data;
};

/**
 * Hủy giao dịch đang PENDING
 */
export const cancelPayment = async (id: number): Promise<Payment> => {
  const response = await axiosClient.patch<Payment>(
    `${BASE_PATH}/${id}/cancel`
  );
  return response.data;
};
