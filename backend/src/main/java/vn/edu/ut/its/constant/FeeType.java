package vn.edu.ut.its.constant;

/**
 * Enum phân loại mục đích thanh toán.
 * RIDE_FEE = cước chuyến đi,
 * BOOKING_DEPOSIT = tiền cọc đặt giữ xe,
 * LOST_TICKET_FINE chỉ dùng để đọc giao dịch lịch sử.
 */
public enum FeeType {
    RIDE_FEE,
    BOOKING_DEPOSIT,
    LOST_TICKET_FINE
}
