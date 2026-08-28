package com.vegetableshop.dto;

import com.vegetableshop.entity.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AdminCategoryRequest {

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 100, message = "Tên danh mục không được vượt quá 100 ký tự")
    private String name;

    @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự")
    private String description;

    @Size(max = 500, message = "Đường dẫn ảnh không được vượt quá 500 ký tự")
    private String image;

    private boolean status = true;

    public static AdminCategoryRequest from(Category category) {
        AdminCategoryRequest request = new AdminCategoryRequest();
        request.setName(category.getName());
        request.setDescription(category.getDescription());
        request.setImage(category.getImage());
        request.setStatus(category.isStatus());
        return request;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }
}
