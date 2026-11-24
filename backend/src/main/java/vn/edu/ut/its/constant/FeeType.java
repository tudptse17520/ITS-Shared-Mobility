package vn.edu.ut.its.constant;

/**
 * Enum phân loại mục đích thanh toán.
 * RIDE_FEE = cước chuyến đi,
 * BOOKING_DEPOSIT = tiền cọc đặt giữ xe,
 * HISTORICAL_ADJUSTMENT chỉ dùng để đọc giao dịch lịch sử.
 */
public enum FeeType {
    RIDE_FEE,
    BOOKING_DEPOSIT,
    HISTORICAL_ADJUSTMENT
}
