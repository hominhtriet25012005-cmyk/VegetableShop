package com.vegetableshop.dto;

import com.vegetableshop.entity.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

public class VoucherAdminRequest {
    @NotBlank @Pattern(regexp="^[A-Za-z0-9_-]{3,30}$", message="Mã gồm 3–30 ký tự chữ, số, _ hoặc -") private String code;
    @NotBlank @Size(max=150) private String name;
    @NotNull private DiscountType discountType = DiscountType.PERCENTAGE;
    @NotNull @DecimalMin("0.01") private BigDecimal discountValue;
    @NotNull @DecimalMin("0") private BigDecimal minimumOrderAmount = BigDecimal.ZERO;
    @DecimalMin("0.01") private BigDecimal maximumDiscountAmount;
    @NotNull @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) private LocalDateTime startsAt = LocalDateTime.now().withSecond(0).withNano(0);
    @NotNull @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) private LocalDateTime endsAt = LocalDateTime.now().plusDays(30).withSecond(0).withNano(0);
    @Min(1) private Integer totalUsageLimit;
    @NotNull @Min(1) private Integer perUserUsageLimit = 1;
    @NotNull private VoucherScopeType scopeType = VoucherScopeType.ORDER;
    private Set<Long> targetIds = new LinkedHashSet<>();
    private boolean status = true;
    public static VoucherAdminRequest from(Voucher v) { var r=new VoucherAdminRequest(); r.code=v.getCode();r.name=v.getName();r.discountType=v.getDiscountType();r.discountValue=v.getDiscountValue();r.minimumOrderAmount=v.getMinimumOrderAmount();r.maximumDiscountAmount=v.getMaximumDiscountAmount();r.startsAt=v.getStartsAt();r.endsAt=v.getEndsAt();r.totalUsageLimit=v.getTotalUsageLimit();r.perUserUsageLimit=v.getPerUserUsageLimit();r.status=v.isStatus(); if(!v.getScopes().isEmpty()){r.scopeType=v.getScopes().getFirst().getScopeType();v.getScopes().stream().map(VoucherScope::getTargetId).filter(java.util.Objects::nonNull).forEach(r.targetIds::add);} return r; }
    public String getCode(){return code;} public void setCode(String v){code=v;} public String getName(){return name;} public void setName(String v){name=v;}
    public DiscountType getDiscountType(){return discountType;} public void setDiscountType(DiscountType v){discountType=v;} public BigDecimal getDiscountValue(){return discountValue;} public void setDiscountValue(BigDecimal v){discountValue=v;}
    public BigDecimal getMinimumOrderAmount(){return minimumOrderAmount;} public void setMinimumOrderAmount(BigDecimal v){minimumOrderAmount=v;} public BigDecimal getMaximumDiscountAmount(){return maximumDiscountAmount;} public void setMaximumDiscountAmount(BigDecimal v){maximumDiscountAmount=v;}
    public LocalDateTime getStartsAt(){return startsAt;} public void setStartsAt(LocalDateTime v){startsAt=v;} public LocalDateTime getEndsAt(){return endsAt;} public void setEndsAt(LocalDateTime v){endsAt=v;}
    public Integer getTotalUsageLimit(){return totalUsageLimit;} public void setTotalUsageLimit(Integer v){totalUsageLimit=v;} public Integer getPerUserUsageLimit(){return perUserUsageLimit;} public void setPerUserUsageLimit(Integer v){perUserUsageLimit=v;}
    public VoucherScopeType getScopeType(){return scopeType;} public void setScopeType(VoucherScopeType v){scopeType=v;} public Set<Long> getTargetIds(){return targetIds;} public void setTargetIds(Set<Long> v){targetIds=v==null?new LinkedHashSet<>():v;} public boolean isStatus(){return status;} public void setStatus(boolean v){status=v;}
}
