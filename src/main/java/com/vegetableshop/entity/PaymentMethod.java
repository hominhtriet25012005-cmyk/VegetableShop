package com.vegetableshop.entity;

public enum PaymentMethod {
    COD,
    VNPAY,
    BANK_TRANSFER;

    public String getDisplayName() { return switch (this) {
        case COD -> "Thanh toán khi nhận hàng";
        case VNPAY -> "VNPay";
        case BANK_TRANSFER -> "Chuyển khoản ngân hàng qua QR";
    }; }
}
