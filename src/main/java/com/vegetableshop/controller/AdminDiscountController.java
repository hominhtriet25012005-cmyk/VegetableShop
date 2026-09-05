package com.vegetableshop.controller;

import com.vegetableshop.dto.*;
import com.vegetableshop.entity.*;
import com.vegetableshop.exception.AdminOperationException;
import com.vegetableshop.service.AdminDiscountService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @Profile("mysql") @RequestMapping("/admin/discounts")
public class AdminDiscountController {
    private final AdminDiscountService service; public AdminDiscountController(AdminDiscountService s){service=s;}
    @GetMapping public String index(Model m){m.addAttribute("vouchers",service.vouchers());m.addAttribute("promotions",service.promotions());return "admin/discounts";}
    @GetMapping("/vouchers/new") public String newVoucher(Model m){m.addAttribute("voucherRequest",new VoucherAdminRequest());voucherModel(m,null);return "admin/voucher-form";}
    @GetMapping("/vouchers/{id}/edit") public String editVoucher(@PathVariable Long id,Model m){m.addAttribute("voucherRequest",service.voucherForm(id));voucherModel(m,id);return "admin/voucher-form";}
    @PostMapping("/vouchers") public String createVoucher(@Valid @ModelAttribute("voucherRequest") VoucherAdminRequest r,BindingResult b,Model m,RedirectAttributes a){return saveVoucher(null,r,b,m,a);}
    @PostMapping("/vouchers/{id}") public String updateVoucher(@PathVariable Long id,@Valid @ModelAttribute("voucherRequest") VoucherAdminRequest r,BindingResult b,Model m,RedirectAttributes a){return saveVoucher(id,r,b,m,a);}
    @PostMapping("/vouchers/{id}/toggle") public String toggleVoucher(@PathVariable Long id,RedirectAttributes a){try{a.addFlashAttribute("successMessage",service.toggleVoucher(id)?"Đã bật voucher":"Đã tắt voucher");}catch(RuntimeException e){a.addFlashAttribute("errorMessage",e.getMessage());}return "redirect:/admin/discounts";}
    @GetMapping("/promotions/new") public String newPromotion(Model m){m.addAttribute("promotionRequest",new PromotionAdminRequest());promotionModel(m,null);return "admin/promotion-form";}
    @GetMapping("/promotions/{id}/edit") public String editPromotion(@PathVariable Long id,Model m){m.addAttribute("promotionRequest",service.promotionForm(id));promotionModel(m,id);return "admin/promotion-form";}
    @PostMapping("/promotions") public String createPromotion(@Valid @ModelAttribute("promotionRequest") PromotionAdminRequest r,BindingResult b,Model m,RedirectAttributes a){return savePromotion(null,r,b,m,a);}
    @PostMapping("/promotions/{id}") public String updatePromotion(@PathVariable Long id,@Valid @ModelAttribute("promotionRequest") PromotionAdminRequest r,BindingResult b,Model m,RedirectAttributes a){return savePromotion(id,r,b,m,a);}
    @PostMapping("/promotions/{id}/toggle") public String togglePromotion(@PathVariable Long id,RedirectAttributes a){try{a.addFlashAttribute("successMessage",service.togglePromotion(id)?"Đã bật khuyến mãi":"Đã tắt khuyến mãi");}catch(RuntimeException e){a.addFlashAttribute("errorMessage",e.getMessage());}return "redirect:/admin/discounts";}
    private String saveVoucher(Long id,VoucherAdminRequest r,BindingResult b,Model m,RedirectAttributes a){if(!b.hasErrors())try{service.saveVoucher(id,r);a.addFlashAttribute("successMessage","Đã lưu voucher");return "redirect:/admin/discounts";}catch(AdminOperationException|EntityNotFoundException e){b.reject("voucher.failed",e.getMessage());}voucherModel(m,id);return "admin/voucher-form";}
    private String savePromotion(Long id,PromotionAdminRequest r,BindingResult b,Model m,RedirectAttributes a){if(!b.hasErrors())try{service.savePromotion(id,r);a.addFlashAttribute("successMessage","Đã lưu chương trình khuyến mãi");return "redirect:/admin/discounts";}catch(AdminOperationException|EntityNotFoundException e){b.reject("promotion.failed",e.getMessage());}promotionModel(m,id);return "admin/promotion-form";}
    private void voucherModel(Model m,Long id){m.addAttribute("voucherId",id);m.addAttribute("discountTypes",DiscountType.values());m.addAttribute("scopeTypes",VoucherScopeType.values());m.addAttribute("products",service.allProducts());m.addAttribute("categories",service.allCategories());m.addAttribute("brands",service.allBrands());}
    private void promotionModel(Model m,Long id){m.addAttribute("promotionId",id);m.addAttribute("discountTypes",DiscountType.values());m.addAttribute("products",service.allProducts());}
}
