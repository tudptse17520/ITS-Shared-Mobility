import axiosClient from "@/lib/axios-client";
import type { Payment, PaymentListResponse } from "@/features/payments/types/payment.type";

export async function getPayments(): Promise<Payment[]> {
  const { data } = await axiosClient.get<PaymentListResponse>("/payments");
  return data.data;
}
