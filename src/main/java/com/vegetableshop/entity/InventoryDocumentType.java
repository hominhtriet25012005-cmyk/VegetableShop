package com.vegetableshop.entity;

public enum InventoryDocumentType {
    INBOUND("Phiếu nhập kho", "PN"),
    OUTBOUND("Phiếu xuất kho", "PX"),
    ADJUSTMENT("Phiếu kiểm kê/điều chỉnh", "KK");

    private final String displayName;
    private final String codePrefix;

    InventoryDocumentType(String displayName, String codePrefix) {
        this.displayName = displayName;
        this.codePrefix = codePrefix;
    }

    public String getDisplayName() { return displayName; }
    public String getCodePrefix() { return codePrefix; }
}
