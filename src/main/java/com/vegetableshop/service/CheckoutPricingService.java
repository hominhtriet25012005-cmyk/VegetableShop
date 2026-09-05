package com.vegetableshop.service;

import com.vegetableshop.dto.CheckoutPricingView;
import com.vegetableshop.entity.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.math.*;
import java.time.LocalDateTime;
import java.util.*;

@Service @Profile("mysql")
public class CheckoutPricingService {
    private final PromotionPricingService promotions; private final VoucherService vouchers;
    public CheckoutPricingService(PromotionPricingService promotions,VoucherService vouchers){this.promotions=promotions;this.vouchers=vouchers;}
    public CheckoutPricingView quote(Map<Product,Integer> quantities,User user,String code,boolean checkout){
        var prices=promotions.prices(quantities.keySet(),LocalDateTime.now());
        Map<Product,BigDecimal> afterPromotion=new LinkedHashMap<>();
        BigDecimal subtotal=BigDecimal.ZERO,promotionDiscount=BigDecimal.ZERO;
        for(var e:quantities.entrySet()){
            var price=prices.get(e.getKey().getId()); BigDecimal q=BigDecimal.valueOf(e.getValue());
            subtotal=subtotal.add(price.original().multiply(q)); promotionDiscount=promotionDiscount.add(price.unitDiscount().multiply(q));
            afterPromotion.put(e.getKey(),price.effective().multiply(q));
        }
        BigDecimal afterPromotions=subtotal.subtract(promotionDiscount);
        VoucherService.Result voucher=checkout?vouchers.evaluateForCheckout(code,user,afterPromotion,afterPromotions):vouchers.evaluate(code,user,afterPromotion,afterPromotions);
        Map<Long,BigDecimal> allocations=allocate(voucher.discount(),voucher.eligibleProductIds(),afterPromotion);
        List<CheckoutPricingView.Line> lines=new ArrayList<>();
        for(var e:quantities.entrySet()){
            Product p=e.getKey();var price=prices.get(p.getId());BigDecimal promo=price.unitDiscount().multiply(BigDecimal.valueOf(e.getValue()));BigDecimal vd=allocations.getOrDefault(p.getId(),BigDecimal.ZERO);BigDecimal total=afterPromotion.get(p).subtract(vd);
            p.setPromotionDisplay(price.effective(),price.promotionName(),price.flashSale());
            lines.add(new CheckoutPricingView.Line(p,e.getValue(),price.original(),price.effective(),promo,vd,total,price.promotionName(),price.flashSale()));
        }
        String message=voucher.voucher()==null?null:"Đã áp dụng "+voucher.voucher().getCode();
        return new CheckoutPricingView(List.copyOf(lines),subtotal,promotionDiscount,voucher.discount(),afterPromotions.subtract(voucher.discount()),voucher.voucher(),message);
    }
    private Map<Long,BigDecimal> allocate(BigDecimal total,Set<Long> eligible,Map<Product,BigDecimal> lines){
        Map<Long,BigDecimal> out=new HashMap<>();if(total.signum()<=0||eligible.isEmpty())return out;
        List<Map.Entry<Product,BigDecimal>> selected=lines.entrySet().stream().filter(e->eligible.contains(e.getKey().getId())).toList();
        BigDecimal base=selected.stream().map(Map.Entry::getValue).reduce(BigDecimal.ZERO,BigDecimal::add),remaining=total;
        for(int i=0;i<selected.size();i++){var e=selected.get(i);BigDecimal part=i==selected.size()-1?remaining:total.multiply(e.getValue()).divide(base,2,RoundingMode.DOWN);part=part.min(e.getValue());out.put(e.getKey().getId(),part);remaining=remaining.subtract(part);}
        return out;
    }
}
