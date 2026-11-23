package vn.edu.ut.pbms.constant;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Enum hình thức thanh toán.
 * Cash = tiền mặt, Momo = ví MoMo, Vnpay = cổng VNPay, Credit_Card = thẻ tín dụng.
 */
public enum PaymentMethod {
    Cash,
    Momo,
    Vnpay,
    Credit_Card;

    @JsonCreator
    public static PaymentMethod fromString(String value) {
        if (value == null) return null;
        for (PaymentMethod method : PaymentMethod.values()) {
            if (method.name().equalsIgnoreCase(value.trim())) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unknown PaymentMethod: " + value);
    }
}
