package com.vegetableshop.dto;

import com.vegetableshop.entity.Brand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AdminBrandRequest {

    @NotBlank(message = "Tên thương hiệu không được để trống")
    @Size(max = 120, message = "Tên thương hiệu không được vượt quá 120 ký tự")
    private String name;

    @Size(max = 500, message = "Đường dẫn logo không được vượt quá 500 ký tự")
    private String logo;

    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    private String description;

    private boolean status = true;

    public static AdminBrandRequest from(Brand brand) {
        AdminBrandRequest request = new AdminBrandRequest();
        request.setName(brand.getName());
        request.setLogo(brand.getLogo());
        request.setDescription(brand.getDescription());
        request.setStatus(brand.isStatus());
        return request;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }
}
