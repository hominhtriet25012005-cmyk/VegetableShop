package com.vegetableshop.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "promotion_products", uniqueConstraints = @UniqueConstraint(
    name = "uk_promotion_product", columnNames = {"promotion_id", "product_id"}))
public class PromotionProduct {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "promotion_id", nullable = false) private Promotion promotion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id", nullable = false) private Product product;
    protected PromotionProduct() {}
    public PromotionProduct(Promotion promotion, Product product) { this.promotion=promotion; this.product=product; }
    public Long getId() { return id; }
    public Promotion getPromotion() { return promotion; }
    public Product getProduct() { return product; }
}
