package com.vegetableshop.dto;

import com.vegetableshop.entity.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

public class PromotionAdminRequest {
    @NotBlank @Size(max=150) private String name;
    @NotNull private DiscountType discountType = DiscountType.PERCENTAGE;
    @NotNull @DecimalMin("0.01") private BigDecimal discountValue;
    @NotNull @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) private LocalDateTime startsAt=LocalDateTime.now().withSecond(0).withNano(0);
    @NotNull @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) private LocalDateTime endsAt=LocalDateTime.now().plusDays(7).withSecond(0).withNano(0);
    @NotEmpty(message="Chọn ít nhất một sản phẩm") private Set<Long> productIds=new LinkedHashSet<>();
    private boolean flashSale; private boolean status=true;
    public static PromotionAdminRequest from(Promotion p){var r=new PromotionAdminRequest();r.name=p.getName();r.discountType=p.getDiscountType();r.discountValue=p.getDiscountValue();r.startsAt=p.getStartsAt();r.endsAt=p.getEndsAt();r.flashSale=p.isFlashSale();r.status=p.isStatus();p.getProducts().forEach(x->r.productIds.add(x.getProduct().getId()));return r;}
    public String getName(){return name;} public void setName(String v){name=v;} public DiscountType getDiscountType(){return discountType;} public void setDiscountType(DiscountType v){discountType=v;} public BigDecimal getDiscountValue(){return discountValue;} public void setDiscountValue(BigDecimal v){discountValue=v;}
    public LocalDateTime getStartsAt(){return startsAt;} public void setStartsAt(LocalDateTime v){startsAt=v;} public LocalDateTime getEndsAt(){return endsAt;} public void setEndsAt(LocalDateTime v){endsAt=v;}
    public Set<Long> getProductIds(){return productIds;} public void setProductIds(Set<Long> v){productIds=v==null?new LinkedHashSet<>():v;} public boolean isFlashSale(){return flashSale;} public void setFlashSale(boolean v){flashSale=v;} public boolean isStatus(){return status;} public void setStatus(boolean v){status=v;}
}
