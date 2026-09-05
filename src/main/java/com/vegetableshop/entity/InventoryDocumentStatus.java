package com.vegetableshop.entity;

public enum InventoryDocumentStatus {
    DRAFT("Bản nháp"),
    POSTED("Đã xác nhận");

    private final String displayName;

    InventoryDocumentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
