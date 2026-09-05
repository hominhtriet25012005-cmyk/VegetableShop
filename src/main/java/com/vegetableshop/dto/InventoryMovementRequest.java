package com.vegetableshop.dto;

import com.vegetableshop.entity.StockMovementType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class InventoryMovementRequest {

    @NotNull(message = "Vui lòng chọn loại nghiệp vụ kho")
    private StockMovementType movementType = StockMovementType.INBOUND;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 0, message = "Số lượng không được âm")
    @Max(value = 1_000_000, message = "Số lượng vượt quá giới hạn cho phép")
    private Integer quantity;

    @NotBlank(message = "Vui lòng nhập lý do")
    @Size(max = 500, message = "Lý do không được vượt quá 500 ký tự")
    private String reason;

    public StockMovementType getMovementType() { return movementType; }
    public void setMovementType(StockMovementType movementType) { this.movementType = movementType; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
