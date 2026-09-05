package com.vegetableshop.controller;

import com.vegetableshop.entity.*;
import com.vegetableshop.service.*;
import com.vegetableshop.exception.OrderOperationException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.http.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;

@Controller @Profile("mysql")
public class BankTransferController {
    private final BankTransferService service;
    private final VietQrService qr;
    public BankTransferController(BankTransferService service,VietQrService qr) { this.service=service;this.qr=qr; }
    @GetMapping("/orders/{id}/payment")
    public String view(@PathVariable Long id,Authentication auth,Model model) {
        model.addAttribute("payment",service.view(id,auth.getName())); return "bank-payment";
    }
    @GetMapping("/orders/{id}/payment/status") @ResponseBody
    public ResponseEntity<java.util.Map<String,Object>> status(@PathVariable Long id, Authentication auth) {
        BankTransferPayment p=service.view(id,auth.getName());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(java.util.Map.of(
            "version", p.getStatusVersion(), "paid", p.getOrder().getPaymentStatus()==PaymentStatus.PAID));
    }
    @GetMapping("/orders/{id}/payment/receipt")
    public String receipt(@PathVariable Long id,Authentication auth,Model model) {
        BankTransferPayment p=service.view(id,auth.getName());
        if(p.getOrder().getPaymentStatus()!=PaymentStatus.PAID)
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT,"Chưa xác nhận thanh toán");
        model.addAttribute("payment",p);return "bank-payment";
    }
    @GetMapping("/orders/{id}/payment/qr.png") @ResponseBody
    public ResponseEntity<byte[]> image(@PathVariable Long id,Authentication auth,@RequestParam(defaultValue="false") boolean download) {
        BankTransferPayment p=service.view(id,auth.getName());
        if (!p.isAwaitingTransfer()) return ResponseEntity.status(409).cacheControl(CacheControl.noStore()).build();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG)
            .header("X-Content-Type-Options","nosniff")
            .header("Content-Disposition",(download?"attachment":"inline")+"; filename=payment-"+id+".png")
            .body(qr.png(p));
    }
    @PostMapping("/orders/{id}/payment/report")
    public String report(@PathVariable Long id,Authentication auth,RedirectAttributes flash) {
        try { service.report(id,auth.getName()); flash.addFlashAttribute("successMessage","Đã ghi nhận thông báo. Cửa hàng sẽ kiểm tra giao dịch thực nhận."); }
        catch(OrderOperationException e) { flash.addFlashAttribute("errorMessage",e.getMessage()); }
        return "redirect:/orders/"+id+"/payment";
    }
    @GetMapping("/admin/bank-payments")
    public String list(Authentication auth,@RequestParam(required=false) PaymentStatus status,
                       @RequestParam(defaultValue="0") int page,Model model) {
        model.addAttribute("payments",service.list(auth.getName(),status,page));
        model.addAttribute("selectedStatus",status); return "admin/bank-payments";
    }
    @GetMapping("/admin/bank-payments/{id}")
    public String adminView(@PathVariable Long id,Authentication auth,Model model) {
        model.addAttribute("payment",service.view(id,auth.getName()));
        return "admin/bank-payment-detail";
    }
    @PostMapping("/admin/bank-payments/{id}/confirm")
    public String confirm(@PathVariable Long id,Authentication auth,@RequestParam BigDecimal receivedAmount,
                          @RequestParam String transactionCode,RedirectAttributes flash) {
        try { service.confirm(id,auth.getName(),receivedAmount,transactionCode); flash.addFlashAttribute("successMessage","Đã xác nhận tiền thực nhận. Không trừ kho thêm lần nữa."); }
        catch(OrderOperationException e) { flash.addFlashAttribute("errorMessage",e.getMessage()); }
        catch(org.springframework.dao.DataIntegrityViolationException e) { flash.addFlashAttribute("errorMessage","Giao dịch đã được ghi nhận cho đơn khác. Vui lòng đối soát lại."); }
        return "redirect:/admin/bank-payments/"+id;
    }
}
