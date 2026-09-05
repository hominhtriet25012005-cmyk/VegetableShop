package com.vegetableshop.dto;

import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.ProductUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class AdminProductRequest {

    @NotBlank(message = "SKU không được để trống")
    @Size(max = 40, message = "SKU không được vượt quá 40 ký tự")
    private String sku;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 150, message = "Tên sản phẩm không được vượt quá 150 ký tự")
    private String name;

    @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự")
    private String description;

    @NotNull(message = "Giá sản phẩm không được để trống")
    @DecimalMin(value = "0.0", message = "Giá sản phẩm không được âm")
    private BigDecimal price;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 0, message = "Số lượng không được âm")
    private Integer quantity = 0;

    @NotNull(message = "Ngưỡng cảnh báo tồn kho không được để trống")
    @Min(value = 0, message = "Ngưỡng cảnh báo tồn kho không được âm")
    @Max(value = 1_000_000, message = "Ngưỡng cảnh báo tồn kho vượt quá giới hạn")
    private Integer lowStockThreshold = 10;

    @Size(max = 500, message = "Đường dẫn ảnh không được vượt quá 500 ký tự")
    private String image;

    @NotNull(message = "Vui lòng chọn danh mục")
    private Long categoryId;

    private Long brandId;

    @NotNull(message = "Vui lòng chọn đơn vị bán")
    private ProductUnit unit = ProductUnit.KILOGRAM;

    @Size(max = 150, message = "Xuất xứ không được vượt quá 150 ký tự")
    private String origin;

    private Long supplierId;

    @Size(max = 4000, message = "Danh sách ảnh phụ quá dài")
    private String additionalImageUrls;

    private boolean status = true;

    public static AdminProductRequest from(Product product) {
        AdminProductRequest request = new AdminProductRequest();
        request.setSku(product.getSku());
        request.setName(product.getName());
        request.setDescription(product.getDescription());
        request.setPrice(product.getPrice());
        request.setQuantity(product.getQuantity());
        request.setLowStockThreshold(product.getLowStockThreshold());
        request.setImage(product.getImage());
        request.setCategoryId(product.getCategory().getId());
        request.setBrandId(product.getBrand() == null ? null : product.getBrand().getId());
        request.setUnit(product.getUnit());
        request.setOrigin(product.getOrigin());
        request.setSupplierId(product.getSupplier() == null ? null : product.getSupplier().getId());
        request.setAdditionalImageUrls(product.getAdditionalImages().stream()
            .map(image -> image.getImageUrl())
            .reduce((first, second) -> first + System.lineSeparator() + second)
            .orElse(""));
        request.setStatus(product.isStatus());
        return request;
    }

    public String getName() { return name; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(Integer lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getBrandId() { return brandId; }
    public void setBrandId(Long brandId) { this.brandId = brandId; }
    public ProductUnit getUnit() { return unit; }
    public void setUnit(ProductUnit unit) { this.unit = unit; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getAdditionalImageUrls() { return additionalImageUrls; }
    public void setAdditionalImageUrls(String additionalImageUrls) { this.additionalImageUrls = additionalImageUrls; }
    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }
}
