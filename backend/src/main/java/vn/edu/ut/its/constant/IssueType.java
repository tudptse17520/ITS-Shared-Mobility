package vn.edu.ut.its.constant;

/**
 * Loại sự cố của chuyến đi.
 * LOST_TICKET chỉ dùng để đọc lịch sử. WRONG_PLATE = sai biển số,
 * OVERTIME = quá thời gian thuê, WRONG_PLACE = trả xe sai vị trí, UNPAID_EXIT = chưa thanh toán cước.
 */
public enum IssueType {
    LOST_TICKET,
    WRONG_PLATE,
    OVERTIME,
    WRONG_PLACE,
    UNPAID_EXIT
}
