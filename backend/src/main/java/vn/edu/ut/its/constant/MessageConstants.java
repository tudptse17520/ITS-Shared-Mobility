package vn.edu.ut.its.constant;

/**
 * Tập trung hóa tất cả thông báo (success / error) cho module Quản lý bảng giá.
 */
public final class MessageConstants {

    private MessageConstants() {
        // Prevent instantiation
    }

    public static final String PRICING_POLICY_LIST_SUCCESS = "Lấy danh sách bảng giá thành công";
    public static final String PRICING_POLICY_CREATE_SUCCESS = "Thêm mới bảng giá thành công";
    public static final String PRICING_POLICY_UPDATE_SUCCESS = "Cập nhật bảng giá thành công";
    public static final String PRICING_POLICY_DELETE_SUCCESS = "Bảng giá không còn hiệu lực";

    public static final String PRICING_POLICY_DUPLICATE = "Bảng giá đã tồn tại cho loại xe này vào ngày áp dụng đã chọn.";

    public static final String PRICING_POLICY_IN_USE_DELETE = "Không thể xóa. Bảng giá này đang được tham chiếu tính phí cho các chuyến đi.";
    public static final String PRICING_POLICY_IN_USE_UPDATE = "Không thể cập nhật. Bảng giá này đang được tham chiếu tính phí cho các chuyến đi.";

    public static final String BUSINESS_RULE_VIOLATION = "BUSINESS_RULE_VIOLATION";

    public static final String PRICING_POLICY_NOT_FOUND = "Không tìm thấy bảng giá với ID: ";
    public static final String VEHICLE_TYPE_NOT_FOUND = "Không tìm thấy loại phương tiện với ID: ";
}
