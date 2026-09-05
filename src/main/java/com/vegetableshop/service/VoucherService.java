package com.vegetableshop.service;

import com.vegetableshop.entity.*;
import com.vegetableshop.exception.OrderOperationException;
import com.vegetableshop.repository.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.LocalDateTime;
import java.util.*;

@Service @Profile("mysql")
public class VoucherService {
    public record Result(Voucher voucher, Set<Long> eligibleProductIds, BigDecimal discount) {}
    private final VoucherRepository vouchers; private final VoucherUsageRepository usages;
    public VoucherService(VoucherRepository vouchers,VoucherUsageRepository usages){this.vouchers=vouchers;this.usages=usages;}

    @Transactional(readOnly=true)
    public Result evaluate(String code, User user, Map<Product,BigDecimal> lineTotals, BigDecimal orderAmount) {
        if(code==null||code.isBlank()) return new Result(null,Set.of(),BigDecimal.ZERO);
        Voucher voucher=vouchers.findByCodeIgnoreCase(code.trim()).orElseThrow(()->new OrderOperationException("Mã voucher không tồn tại"));
        return evaluate(voucher,user,lineTotals,orderAmount,LocalDateTime.now());
    }
    public Result evaluateForCheckout(String code,User user,Map<Product,BigDecimal> lineTotals,BigDecimal orderAmount){
        if(code==null||code.isBlank()) return new Result(null,Set.of(),BigDecimal.ZERO);
        Voucher v=vouchers.findByCodeForUpdate(code.trim()).orElseThrow(()->new OrderOperationException("Mã voucher không tồn tại"));
        return evaluate(v,user,lineTotals,orderAmount,LocalDateTime.now());
    }
    private Result evaluate(Voucher v,User user,Map<Product,BigDecimal> lines,BigDecimal orderAmount,LocalDateTime now){
        if(!v.isStatus()||now.isBefore(v.getStartsAt())||now.isAfter(v.getEndsAt())) throw new OrderOperationException("Voucher chưa đến thời gian hoặc đã hết hạn");
        if(orderAmount.compareTo(v.getMinimumOrderAmount())<0) throw new OrderOperationException("Đơn hàng chưa đạt giá trị tối thiểu của voucher");
        if(v.getTotalUsageLimit()!=null&&usages.countByVoucherIdAndActiveTrue(v.getId())>=v.getTotalUsageLimit()) throw new OrderOperationException("Voucher đã hết lượt sử dụng");
        if(usages.countByVoucherIdAndUserIdAndActiveTrue(v.getId(),user.getId())>=v.getPerUserUsageLimit()) throw new OrderOperationException("Bạn đã hết lượt sử dụng voucher này");
        Set<Long> eligible=new LinkedHashSet<>();
        for(Product p:lines.keySet()) if(matches(v,p)) eligible.add(p.getId());
        BigDecimal eligibleAmount=lines.entrySet().stream().filter(e->eligible.contains(e.getKey().getId())).map(Map.Entry::getValue).reduce(BigDecimal.ZERO,BigDecimal::add);
        if(eligibleAmount.signum()<=0) throw new OrderOperationException("Voucher không áp dụng cho sản phẩm trong giỏ");
        BigDecimal discount=PromotionPricingService.discount(eligibleAmount,v.getDiscountType(),v.getDiscountValue());
        if(v.getMaximumDiscountAmount()!=null) discount=discount.min(v.getMaximumDiscountAmount());
        return new Result(v,Set.copyOf(eligible),discount.setScale(2,RoundingMode.HALF_UP));
    }
    private boolean matches(Voucher voucher,Product p){
        for(VoucherScope s:voucher.getScopes()) switch(s.getScopeType()){
            case ORDER -> {return true;}
            case PRODUCT -> {if(Objects.equals(s.getTargetId(),p.getId()))return true;}
            case CATEGORY -> {if(p.getCategory()!=null&&Objects.equals(s.getTargetId(),p.getCategory().getId()))return true;}
            case BRAND -> {if(p.getBrand()!=null&&Objects.equals(s.getTargetId(),p.getBrand().getId()))return true;}
        }
        return false;
    }
    @Transactional
    public void record(Voucher voucher,User user,Order order,BigDecimal amount){if(voucher==null||amount.signum()<=0)return;var u=new VoucherUsage();u.setVoucher(voucher);u.setUser(user);u.setOrder(order);u.setDiscountAmount(amount);usages.save(u);}
    @Transactional
    public void releaseForCancelledOrder(Long orderId){usages.findByOrderIdAndActiveTrue(orderId).ifPresent(u->{u.setActive(false);u.setReleasedAt(LocalDateTime.now());});}
}
