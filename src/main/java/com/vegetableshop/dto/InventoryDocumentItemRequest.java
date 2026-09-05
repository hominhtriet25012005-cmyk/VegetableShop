package com.vegetableshop.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class InventoryDocumentItemRequest {

    @NotNull(message = "Vui lòng chọn sản phẩm")
    private Long productId;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 0, message = "Số lượng không được âm")
    @Max(value = 1_000_000, message = "Số lượng vượt quá giới hạn")
    private Integer quantity;

    @DecimalMin(value = "0.0", inclusive = true, message = "Giá vốn không được âm")
    private BigDecimal unitCost;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
}
