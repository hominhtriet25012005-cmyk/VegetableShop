package com.vegetableshop.entity;

public enum StockMovementType {
    INITIAL("Khởi tạo tồn kho"),
    INBOUND("Nhập kho"),
    OUTBOUND("Xuất kho"),
    ADJUSTMENT("Điều chỉnh kiểm kê"),
    SALE("Xuất bán hàng"),
    RETURN("Hoàn kho từ đơn hủy");

    private final String displayName;

    StockMovementType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isManual() {
        return this == INBOUND || this == OUTBOUND || this == ADJUSTMENT;
    }
}
