package com.vegetableshop.dto;

import com.vegetableshop.entity.Supplier;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AdminSupplierRequest {

    @NotBlank(message = "Mã nhà cung cấp không được để trống")
    @Size(max = 30, message = "Mã nhà cung cấp không được vượt quá 30 ký tự")
    private String code;

    @NotBlank(message = "Tên nhà cung cấp không được để trống")
    @Size(max = 150, message = "Tên nhà cung cấp không được vượt quá 150 ký tự")
    private String name;

    @Size(max = 100, message = "Tên người liên hệ không được vượt quá 100 ký tự")
    private String contactPerson;

    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    private String phone;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 150, message = "Email không được vượt quá 150 ký tự")
    private String email;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String address;

    @Size(max = 30, message = "Mã số thuế không được vượt quá 30 ký tự")
    private String taxCode;

    private boolean status = true;

    public static AdminSupplierRequest from(Supplier supplier) {
        AdminSupplierRequest request = new AdminSupplierRequest();
        request.setCode(supplier.getCode());
        request.setName(supplier.getName());
        request.setContactPerson(supplier.getContactPerson());
        request.setPhone(supplier.getPhone());
        request.setEmail(supplier.getEmail());
        request.setAddress(supplier.getAddress());
        request.setTaxCode(supplier.getTaxCode());
        request.setStatus(supplier.isStatus());
        return request;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getTaxCode() { return taxCode; }
    public void setTaxCode(String taxCode) { this.taxCode = taxCode; }
    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }
}
