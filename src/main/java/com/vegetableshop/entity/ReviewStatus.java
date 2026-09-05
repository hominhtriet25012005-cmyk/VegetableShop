package com.vegetableshop.entity;

public enum ReviewStatus {
    PENDING("Chờ duyệt"), APPROVED("Đã duyệt"), HIDDEN("Đã ẩn"), DELETED("Đã xóa");
    private final String displayName;
    ReviewStatus(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
