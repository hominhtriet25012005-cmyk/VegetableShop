package com.vegetableshop.controller;
import com.vegetableshop.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.jsoup.Jsoup;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("template")
@Import(BankPaymentRenderingTests.Preview.class)
class BankPaymentRenderingTests {
    @Autowired MockMvc mvc;
    @Test @WithMockUser void qrPageHasPrivateImageLinkReportCsrfAndNoAutomaticPayment() throws Exception {
        var doc=render("/__p13/payment","payment.html");
        assertNotNull(doc.selectFirst("img[src='/orders/1/payment/qr.png']"));
        assertNotNull(doc.selectFirst("form[action='/orders/1/payment/report'] input[name=_csrf]"));
        assertTrue(doc.text().contains("185.000 đ"));assertTrue(doc.text().contains("Chưa thanh toán"));
        assertFalse(doc.html().contains("img.vietqr.io"));
    }
    @Test @WithMockUser(roles="ADMIN") void adminReviewFormAndFiltersRender() throws Exception {
        var list=render("/__p13/admin","payments.html");assertNotNull(list.selectFirst("select[name=status] option[value=REPORTED]"));
        var detail=render("/__p13/admin-detail","payment-admin.html");
        assertNotNull(detail.selectFirst("form[action='/admin/bank-payments/1/confirm'] input[name=_csrf]"));
        assertNotNull(detail.selectFirst("input[name=receivedAmount][required]"));assertNotNull(detail.selectFirst("input[name=transactionCode][required]"));
    }
    @Test void anonymousCannotReadQr() throws Exception {mvc.perform(get("/orders/1/payment/qr.png")).andExpect(status().is3xxRedirection());}
    @Test @WithMockUser void paidReceiptRendersWithoutQrAndHasPrintButton() throws Exception {
        var doc=render("/__p13/receipt","receipt.html");assertNotNull(doc.selectFirst("#payment-receipt"));
        assertNotNull(doc.selectFirst("[data-print-receipt]"));assertNull(doc.selectFirst("img[src*='/payment/qr.png']"));
        assertTrue(doc.text().contains("TX-MANUAL42"));assertTrue(doc.text().contains("không phải biên lai ngân hàng"));
    }
    @Test @WithMockUser void customerCannotOpenAdminOrConfirmWithoutCsrf() throws Exception {
        mvc.perform(get("/admin/bank-payments")).andExpect(status().isForbidden());
        mvc.perform(post("/orders/1/payment/report")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/bank-payments/1/confirm")).andExpect(status().isForbidden());
    }
    private org.jsoup.nodes.Document render(String url,String file) throws Exception {
        String html=mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        if(Boolean.getBoolean("payments.preview")) {Path folder=Path.of("target/review-preview");Files.createDirectories(folder);Files.writeString(folder.resolve(file),html);Files.write(folder.resolve("qr.png"),new com.vegetableshop.service.VietQrService().png(new Preview().payment()));}
        return Jsoup.parse(html);
    }
    @Controller static class Preview {
        BankTransferPayment payment() {
            var u=new User();u.setId(1L);u.setFullName("Khách minh họa");u.setEmail("test@example.com");
            var o=new Order();o.setId(1L);o.setUser(u);o.setOrderCode("VS-20260904120000-AABBCCDD");o.setPaymentMethod(PaymentMethod.BANK_TRANSFER);o.setPaymentStatus(PaymentStatus.UNPAID);o.setStatus(OrderStatus.PENDING);
            var p=new BankTransferPayment();p.setOrder(o);p.setBankBin("970422");p.setBankName("MB — dữ liệu minh họa");p.setAccountName("NGUOI NHAN THU NGHIEM");p.setAccountNumber("0000000000");p.setReference("VS20260904120000AABBCCDD");p.setAmount(new BigDecimal("185000"));return p;
        }
        @GetMapping("/__p13/payment") String publicView(Model m) {m.addAttribute("payment",payment());return "bank-payment";}
        @GetMapping("/__p13/admin") String list(Model m) {m.addAttribute("payments",new PageImpl<>(List.of(payment())));return "admin/bank-payments";}
        @GetMapping("/__p13/admin-detail") String detail(Model m) {m.addAttribute("payment",payment());return "admin/bank-payment-detail";}
        @GetMapping("/__p13/receipt") String receipt(Model m) {var p=payment();p.getOrder().setPaymentStatus(PaymentStatus.PAID);p.getOrder().setPaymentTransactionCode("TX-MANUAL42");p.getOrder().setPaidAt(java.time.LocalDateTime.now());m.addAttribute("payment",p);return "bank-payment";}
    }
}
