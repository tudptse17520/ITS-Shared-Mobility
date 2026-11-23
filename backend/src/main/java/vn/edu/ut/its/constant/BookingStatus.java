package vn.edu.ut.its.constant;

/**
 * Status enum for Booking entity.
 * PENDING = chờ xác nhận, CONFIRMED = đã xác nhận,
 * CHECKED_IN = lượt giữ đã được sử dụng, CANCELLED = đã hủy, EXPIRED = đã hết hạn.
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CHECKED_IN,
    CANCELLED,
    EXPIRED
}
