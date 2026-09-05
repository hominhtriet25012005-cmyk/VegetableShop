package com.vegetableshop.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "promotions")
public class Promotion extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 150) private String name;
    @Enumerated(EnumType.STRING) @Column(name = "discount_type", nullable = false, length = 20) private DiscountType discountType;
    @Column(name = "discount_value", nullable = false, precision = 15, scale = 2) private BigDecimal discountValue;
    @Column(name = "starts_at", nullable = false) private LocalDateTime startsAt;
    @Column(name = "ends_at", nullable = false) private LocalDateTime endsAt;
    @Column(name = "flash_sale", nullable = false) private boolean flashSale;
    @Column(nullable = false) private boolean status = true;
    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true) private List<PromotionProduct> products = new ArrayList<>();
    public void replaceProducts(Iterable<Product> values) { products.clear(); for (Product p : values) products.add(new PromotionProduct(this, p)); }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }
    public LocalDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(LocalDateTime endsAt) { this.endsAt = endsAt; }
    public boolean isFlashSale() { return flashSale; }
    public void setFlashSale(boolean flashSale) { this.flashSale = flashSale; }
    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }
    public List<PromotionProduct> getProducts() { return products; }
    public boolean isCurrentlyActive() { LocalDateTime n=LocalDateTime.now(); return status&&!n.isBefore(startsAt)&&!n.isAfter(endsAt); }
}
