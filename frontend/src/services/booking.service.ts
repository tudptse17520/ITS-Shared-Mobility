import axiosClient from "@/lib/axios-client";
import type {
  BookingResponse,
  BookingListResponse,
} from "@/features/bookings/types/booking.type";

const BASE_PATH = "/bookings";

export const getAllBookings = async (): Promise<BookingListResponse[]> => {
  const response = await axiosClient.get<BookingListResponse[]>(BASE_PATH);
  return response.data;
};

export const cancelBooking = async (bookingId: number): Promise<void> => {
  await axiosClient.put(`${BASE_PATH}/${bookingId}/cancel`);
};

export const updateBookingStatus = async (
  bookingId: number,
  status: string
): Promise<BookingResponse> => {
  const response = await axiosClient.patch<BookingResponse>(
    `${BASE_PATH}/${bookingId}/status`,
    { status }
  );
  return response.data;
};

export const deleteBooking = async (bookingId: number): Promise<void> => {
  await axiosClient.delete(`${BASE_PATH}/${bookingId}`);
};
