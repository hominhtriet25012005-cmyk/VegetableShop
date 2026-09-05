package com.vegetableshop.entity;

public enum PaymentStatus {
    UNPAID,
    PAID,
    FAILED,
    REFUNDED,
    REPORTED;

    public String getDisplayName() { return switch (this) {
        case UNPAID -> "Chưa thanh toán";
        case REPORTED -> "Chuyển khoản đang chờ đối soát";
        case PAID -> "Đã thanh toán";
        case FAILED -> "Thanh toán thất bại";
        case REFUNDED -> "Đã hoàn tiền";
    }; }
}
