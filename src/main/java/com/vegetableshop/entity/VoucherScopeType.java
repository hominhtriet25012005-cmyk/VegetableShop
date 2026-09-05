package com.vegetableshop.entity;

public enum VoucherScopeType {
    ORDER("Toàn đơn hàng"),
    CATEGORY("Danh mục"),
    BRAND("Thương hiệu"),
    PRODUCT("Sản phẩm");

    private final String displayName;

    VoucherScopeType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
