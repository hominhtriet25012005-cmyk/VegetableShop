package com.vegetableshop.service;

import com.vegetableshop.dto.*;
import com.vegetableshop.entity.*;
import com.vegetableshop.exception.AdminOperationException;
import com.vegetableshop.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service @Profile("mysql")
public class AdminDiscountService {
    private final VoucherRepository vouchers; private final PromotionRepository promotions;
    private final ProductRepository products; private final CategoryRepository categories; private final BrandRepository brands;
    public AdminDiscountService(VoucherRepository v,PromotionRepository p,ProductRepository products,CategoryRepository categories,BrandRepository brands){this.vouchers=v;this.promotions=p;this.products=products;this.categories=categories;this.brands=brands;}
    @Transactional(readOnly=true) public List<Voucher> vouchers(){return vouchers.findAllByOrderByCreatedAtDesc();}
    @Transactional(readOnly=true) public List<Promotion> promotions(){return promotions.findAllByOrderByCreatedAtDesc();}
    @Transactional(readOnly=true) public VoucherAdminRequest voucherForm(Long id){return VoucherAdminRequest.from(vouchers.findById(id).orElseThrow(()->new EntityNotFoundException("Không tìm thấy voucher")));}
    @Transactional(readOnly=true) public PromotionAdminRequest promotionForm(Long id){return PromotionAdminRequest.from(promotions.findDetailedById(id).orElseThrow(()->new EntityNotFoundException("Không tìm thấy khuyến mãi")));}
    @Transactional public Voucher saveVoucher(Long id,VoucherAdminRequest r){validateVoucher(id,r);Voucher v=id==null?new Voucher():vouchers.findById(id).orElseThrow(()->new EntityNotFoundException("Không tìm thấy voucher"));v.setCode(r.getCode().trim().toUpperCase(Locale.ROOT));v.setName(r.getName().trim());v.setDiscountType(r.getDiscountType());v.setDiscountValue(r.getDiscountValue());v.setMinimumOrderAmount(r.getMinimumOrderAmount());v.setMaximumDiscountAmount(r.getMaximumDiscountAmount());v.setStartsAt(r.getStartsAt());v.setEndsAt(r.getEndsAt());v.setTotalUsageLimit(r.getTotalUsageLimit());v.setPerUserUsageLimit(r.getPerUserUsageLimit());v.setStatus(r.isStatus());v.replaceScopes(r.getScopeType(),r.getTargetIds());return vouchers.save(v);}
    @Transactional public Promotion savePromotion(Long id,PromotionAdminRequest r){validateDiscount(r.getDiscountType(),r.getDiscountValue());if(r.getEndsAt()==null||r.getStartsAt()==null||!r.getEndsAt().isAfter(r.getStartsAt()))throw new AdminOperationException("Thời gian kết thúc phải sau thời gian bắt đầu");List<Product> selected=products.findAllById(r.getProductIds());if(selected.size()!=r.getProductIds().size())throw new AdminOperationException("Có sản phẩm không tồn tại");Promotion p=id==null?new Promotion():promotions.findDetailedById(id).orElseThrow(()->new EntityNotFoundException("Không tìm thấy khuyến mãi"));p.setName(r.getName().trim());p.setDiscountType(r.getDiscountType());p.setDiscountValue(r.getDiscountValue());p.setStartsAt(r.getStartsAt());p.setEndsAt(r.getEndsAt());p.setFlashSale(r.isFlashSale());p.setStatus(r.isStatus());p.replaceProducts(selected);return promotions.save(p);}
    @Transactional public boolean toggleVoucher(Long id){Voucher v=vouchers.findById(id).orElseThrow(()->new EntityNotFoundException("Không tìm thấy voucher"));v.setStatus(!v.isStatus());return v.isStatus();}
    @Transactional public boolean togglePromotion(Long id){Promotion p=promotions.findById(id).orElseThrow(()->new EntityNotFoundException("Không tìm thấy khuyến mãi"));p.setStatus(!p.isStatus());return p.isStatus();}
    @Transactional(readOnly=true) public List<Product> allProducts(){return products.findAllByOrderByNameAsc();}
    @Transactional(readOnly=true) public List<Category> allCategories(){return categories.findAll(org.springframework.data.domain.Sort.by("name"));}
    @Transactional(readOnly=true) public List<Brand> allBrands(){return brands.findAllByOrderByNameAsc();}
    private void validateVoucher(Long id,VoucherAdminRequest r){validateDiscount(r.getDiscountType(),r.getDiscountValue());if(r.getEndsAt()==null||r.getStartsAt()==null||!r.getEndsAt().isAfter(r.getStartsAt()))throw new AdminOperationException("Thời gian kết thúc phải sau thời gian bắt đầu");if(id==null?vouchers.existsByCodeIgnoreCase(r.getCode()):vouchers.existsByCodeIgnoreCaseAndIdNot(r.getCode(),id))throw new AdminOperationException("Mã voucher đã tồn tại");if(r.getScopeType()!=VoucherScopeType.ORDER&&(r.getTargetIds()==null||r.getTargetIds().isEmpty()))throw new AdminOperationException("Phải chọn ít nhất một đối tượng áp dụng");if(r.getScopeType()==VoucherScopeType.CATEGORY&&categories.findAllById(r.getTargetIds()).size()!=r.getTargetIds().size())throw new AdminOperationException("Có danh mục không tồn tại");if(r.getScopeType()==VoucherScopeType.BRAND&&brands.findAllById(r.getTargetIds()).size()!=r.getTargetIds().size())throw new AdminOperationException("Có thương hiệu không tồn tại");if(r.getScopeType()==VoucherScopeType.PRODUCT&&products.findAllById(r.getTargetIds()).size()!=r.getTargetIds().size())throw new AdminOperationException("Có sản phẩm không tồn tại");}
    private void validateDiscount(DiscountType type,BigDecimal value){if(type==null||value==null||value.signum()<=0)throw new AdminOperationException("Giá trị giảm phải lớn hơn 0");if(type==DiscountType.PERCENTAGE&&value.compareTo(BigDecimal.valueOf(100))>0)throw new AdminOperationException("Mức giảm phần trăm không được vượt quá 100%");}
}
