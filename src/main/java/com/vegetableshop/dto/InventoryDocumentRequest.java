package com.vegetableshop.dto;

import com.vegetableshop.entity.InventoryDocumentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class InventoryDocumentRequest {

    @NotNull(message = "Vui lòng chọn loại phiếu")
    private InventoryDocumentType type = InventoryDocumentType.INBOUND;

    private Long supplierId;

    @Size(max = 100, message = "Mã hóa đơn/tham chiếu không được vượt quá 100 ký tự")
    private String invoiceReference;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    private String note;

    @Valid
    private List<InventoryDocumentItemRequest> items = new ArrayList<>();

    public InventoryDocumentRequest() {
        items.add(new InventoryDocumentItemRequest());
    }

    public InventoryDocumentType getType() { return type; }
    public void setType(InventoryDocumentType type) { this.type = type; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getInvoiceReference() { return invoiceReference; }
    public void setInvoiceReference(String invoiceReference) { this.invoiceReference = invoiceReference; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public List<InventoryDocumentItemRequest> getItems() { return items; }
    public void setItems(List<InventoryDocumentItemRequest> items) { this.items = items; }
}
