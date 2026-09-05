package com.vegetableshop.entity;

public enum DiscountType {
    PERCENTAGE("Phần trăm"),
    FIXED_AMOUNT("Số tiền cố định");

    private final String displayName;

    DiscountType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
