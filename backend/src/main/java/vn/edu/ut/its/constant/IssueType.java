package vn.edu.ut.its.constant;

/**
 * Loại sự cố của chuyến đi.
 * OTHER = sự cố khác, WRONG_PLATE = sai biển số,
 * RIDE_OVERTIME = quá thời gian thuê, WRONG_RETURN_LOCATION = trả xe sai vị trí, UNPAID_RIDE = chưa thanh toán cước.
 */
public enum IssueType {
    OTHER,
    WRONG_PLATE,
    RIDE_OVERTIME,
    WRONG_RETURN_LOCATION,
    UNPAID_RIDE
}
