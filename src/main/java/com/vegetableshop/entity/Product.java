package com.vegetableshop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40, unique = true)
    private String sku;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 150, message = "Tên sản phẩm không được vượt quá 150 ký tự")
    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Giá sản phẩm không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Giá sản phẩm không được âm")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @NotNull(message = "Số lượng sản phẩm không được để trống")
    @Min(value = 0, message = "Số lượng sản phẩm không được âm")
    @Column(nullable = false)
    private Integer quantity = 0;

    @NotNull(message = "Ngưỡng cảnh báo tồn kho không được để trống")
    @Min(value = 0, message = "Ngưỡng cảnh báo tồn kho không được âm")
    @Column(name = "low_stock_threshold", nullable = false)
    private Integer lowStockThreshold = 10;

    @Size(max = 500, message = "Đường dẫn ảnh không được vượt quá 500 ký tự")
    @Column(length = 500)
    private String image;

    @NotNull(message = "Sản phẩm phải thuộc một danh mục")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false, length = 30)
    private ProductUnit unit = ProductUnit.KILOGRAM;

    @Size(max = 150, message = "Xuất xứ không được vượt quá 150 ký tự")
    @Column(length = 150)
    private String origin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "created_by", nullable = false, length = 150)
    private String createdBy = "SYSTEM";

    @Column(name = "updated_by", nullable = false, length = 150)
    private String updatedBy = "SYSTEM";

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<ProductImage> additionalImages = new ArrayList<>();

    @Column(nullable = false)
    private boolean status = true;

    @Transient private BigDecimal effectivePrice;
    @Transient private String promotionName;
    @Transient private boolean flashSale;

    @PrePersist
    void initializeCatalogFields() {
        if (sku == null || sku.isBlank()) {
            sku = "SP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        if (unit == null) {
            unit = ProductUnit.KILOGRAM;
        }
        if (createdBy == null || createdBy.isBlank()) {
            createdBy = "SYSTEM";
        }
        if (updatedBy == null || updatedBy.isBlank()) {
            updatedBy = createdBy;
        }
    }

    public Long getId() {
        return id;
    }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Brand getBrand() { return brand; }
    public void setBrand(Brand brand) { this.brand = brand; }
    public ProductUnit getUnit() { return unit; }
    public void setUnit(ProductUnit unit) { this.unit = unit; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public List<ProductImage> getAdditionalImages() { return additionalImages; }

    public void replaceAdditionalImages(List<String> imageUrls) {
        additionalImages.clear();
        int index = 0;
        for (String imageUrl : imageUrls) {
            ProductImage image = new ProductImage();
            image.setProduct(this);
            image.setImageUrl(imageUrl);
            image.setDisplayOrder(index++);
            additionalImages.add(image);
        }
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public BigDecimal getEffectivePrice() { return effectivePrice == null ? price : effectivePrice; }
    public void setPromotionDisplay(BigDecimal value, String name, boolean flash) {
        effectivePrice = value; promotionName = name; flashSale = flash;
    }
    public boolean isOnPromotion() { return effectivePrice != null && price != null && effectivePrice.compareTo(price) < 0; }
    public String getPromotionName() { return promotionName; }
    public boolean isFlashSale() { return flashSale; }
}
