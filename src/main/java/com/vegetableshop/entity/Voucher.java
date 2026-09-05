package com.vegetableshop.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vouchers")
public class Voucher extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 30) private String code;
    @Column(nullable = false, length = 150) private String name;
    @Enumerated(EnumType.STRING) @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;
    @Column(name = "discount_value", nullable = false, precision = 15, scale = 2) private BigDecimal discountValue;
    @Column(name = "minimum_order_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal minimumOrderAmount = BigDecimal.ZERO;
    @Column(name = "maximum_discount_amount", precision = 15, scale = 2) private BigDecimal maximumDiscountAmount;
    @Column(name = "starts_at", nullable = false) private LocalDateTime startsAt;
    @Column(name = "ends_at", nullable = false) private LocalDateTime endsAt;
    @Column(name = "total_usage_limit") private Integer totalUsageLimit;
    @Column(name = "per_user_usage_limit", nullable = false) private Integer perUserUsageLimit = 1;
    @Column(nullable = false) private boolean status = true;
    @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VoucherScope> scopes = new ArrayList<>();

    public void replaceScopes(VoucherScopeType type, Iterable<Long> targetIds) {
        scopes.clear();
        if (type == VoucherScopeType.ORDER) {
            scopes.add(new VoucherScope(this, type, null));
            return;
        }
        for (Long targetId : targetIds) scopes.add(new VoucherScope(this, type, targetId));
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
    public BigDecimal getMinimumOrderAmount() { return minimumOrderAmount; }
    public void setMinimumOrderAmount(BigDecimal minimumOrderAmount) { this.minimumOrderAmount = minimumOrderAmount; }
    public BigDecimal getMaximumDiscountAmount() { return maximumDiscountAmount; }
    public void setMaximumDiscountAmount(BigDecimal maximumDiscountAmount) { this.maximumDiscountAmount = maximumDiscountAmount; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }
    public LocalDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(LocalDateTime endsAt) { this.endsAt = endsAt; }
    public Integer getTotalUsageLimit() { return totalUsageLimit; }
    public void setTotalUsageLimit(Integer totalUsageLimit) { this.totalUsageLimit = totalUsageLimit; }
    public Integer getPerUserUsageLimit() { return perUserUsageLimit; }
    public void setPerUserUsageLimit(Integer perUserUsageLimit) { this.perUserUsageLimit = perUserUsageLimit; }
    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }
    public List<VoucherScope> getScopes() { return scopes; }
    public boolean isCurrentlyActive() {
        LocalDateTime now = LocalDateTime.now();
        return status && !now.isBefore(startsAt) && !now.isAfter(endsAt);
    }
}
