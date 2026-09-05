package com.vegetableshop.service;

import com.vegetableshop.entity.*;
import com.vegetableshop.repository.PromotionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Profile("mysql")
public class PromotionPricingService {
    public record Price(BigDecimal original, BigDecimal effective, BigDecimal unitDiscount,
                        String promotionName, boolean flashSale) {}
    private final PromotionRepository promotions;
    public PromotionPricingService(PromotionRepository promotions) { this.promotions=promotions; }

    @Transactional(readOnly=true)
    public Map<Long,Price> prices(Collection<Product> products, LocalDateTime now) {
        Map<Long,Product> byId=new LinkedHashMap<>();
        products.stream().filter(Objects::nonNull).filter(p->p.getId()!=null).forEach(p->byId.put(p.getId(),p));
        Map<Long,Price> result=new HashMap<>();
        byId.values().forEach(p->result.put(p.getId(),regular(p)));
        if(byId.isEmpty()) return result;
        for(Promotion promotion:promotions.findByStatusTrueAndStartsAtLessThanEqualAndEndsAtGreaterThanEqual(now,now)) {
            for(PromotionProduct link:promotion.getProducts()) {
                Product product=byId.get(link.getProduct().getId());
                if(product==null) continue;
                BigDecimal discount=discount(product.getPrice(),promotion.getDiscountType(),promotion.getDiscountValue());
                BigDecimal effective=product.getPrice().subtract(discount).max(BigDecimal.ZERO).setScale(2,RoundingMode.HALF_UP);
                Price current=result.get(product.getId());
                if(effective.compareTo(current.effective())<0)
                    result.put(product.getId(),new Price(product.getPrice(),effective,product.getPrice().subtract(effective),promotion.getName(),promotion.isFlashSale()));
            }
        }
        return result;
    }

    @Transactional(readOnly=true)
    public void decorate(Collection<Product> products) {
        Map<Long,Price> values=prices(products,LocalDateTime.now());
        products.forEach(p->{Price v=values.get(p.getId());if(v!=null)p.setPromotionDisplay(v.effective(),v.promotionName(),v.flashSale());});
    }

    static BigDecimal discount(BigDecimal base, DiscountType type, BigDecimal value) {
        if(type==DiscountType.PERCENTAGE) return base.multiply(value).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP).min(base);
        return value.min(base).setScale(2,RoundingMode.HALF_UP);
    }
    private Price regular(Product p){return new Price(p.getPrice(),p.getPrice(),BigDecimal.ZERO,null,false);}
}
