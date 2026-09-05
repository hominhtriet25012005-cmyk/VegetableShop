package com.vegetableshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CheckoutRequest {

    @NotBlank(message = "Tên người nhận không được để trống")
    @Size(max = 100, message = "Tên người nhận không được vượt quá 100 ký tự")
    private String receiverName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^[0-9+ .()-]{8,20}$", message = "Số điện thoại không đúng định dạng")
    private String receiverPhone;

    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String shippingAddress;

    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String note;

    @NotBlank(message = "Vui lòng chọn phương thức thanh toán")
    @Pattern(regexp = "^(COD|BANK_TRANSFER)$", message = "Phương thức thanh toán không được hỗ trợ")
    private String paymentMethod = "COD";

    @Size(max = 30, message = "Mã voucher không được vượt quá 30 ký tự")
    @Pattern(regexp = "^$|^[A-Za-z0-9_-]{3,30}$", message = "Mã voucher không đúng định dạng")
    private String voucherCode;

    @NotBlank
    @Pattern(regexp = "^[a-fA-F0-9-]{36}$")
    private String checkoutToken = java.util.UUID.randomUUID().toString();
    public String getCheckoutToken() { return checkoutToken; }
    public void setCheckoutToken(String token) { this.checkoutToken = token; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }
}
