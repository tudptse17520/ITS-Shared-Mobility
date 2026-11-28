package vn.edu.ut.its.constant;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PaymentMethod {
    Cash,
    // Retained for reading historical payment records.
    Momo,
    Vnpay,
    // Retained for reading historical payment records.
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
