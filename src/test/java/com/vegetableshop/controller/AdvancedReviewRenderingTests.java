package com.vegetableshop.controller;

import com.vegetableshop.entity.*;
import com.vegetableshop.dto.ReviewSummary;
import org.jsoup.Jsoup;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("template")
@Import(AdvancedReviewRenderingTests.Preview.class)
class AdvancedReviewRenderingTests {
    @Autowired MockMvc mvc;
    @Test @WithMockUser(roles="USER")
    void publicReviewFormGalleryHistogramAndPrivateStateRenderSafely() throws Exception {
        var doc = render("/__test/phase19/product", "product.html");
        var form = doc.selectFirst("form[data-review-form]");
        assertNotNull(form); assertEquals("multipart/form-data", form.attr("enctype"));
        assertNotNull(form.selectFirst("input[name=_csrf]"));
        assertNotNull(form.selectFirst("select[name=orderDetailId] option[value=10]"));
        assertNotNull(form.selectFirst("input[type=file][multiple]"));
        assertEquals(5, doc.select("#reviews progress").size());
        assertTrue(doc.select("#reviews").text().contains("Đã mua hàng"));
        assertTrue(doc.select("#reviews").text().contains("Chờ duyệt"));
        assertTrue(doc.select("#reviews").text().contains("<script>unsafe</script>"));
        assertFalse(doc.html().contains("<script>unsafe</script>"));
        assertFalse(doc.select("#reviews img[src='/review-images/1']").isEmpty());
    }
    @Test @WithMockUser(roles="ADMIN")
    void adminListAndDetailRenderFiltersModerationAndPhotos() throws Exception {
        var list = render("/__test/phase19/admin", "admin.html");
        assertEquals(5, list.select("select[name=status] option").size());
        assertTrue(list.text().contains("Nấm kiểm thử"));
        var detail = render("/__test/phase19/detail", "admin-detail.html");
        assertNotNull(detail.selectFirst("form[action='/admin/reviews/1/moderate'] input[name=_csrf]"));
        assertNotNull(detail.selectFirst("option[value=DELETED]"));
        assertTrue(detail.text().contains("Đã mua hàng"));
        assertFalse(detail.html().contains("<script>unsafe</script>"));
    }
    private org.jsoup.nodes.Document render(String url, String name) throws Exception {
        String html = mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        if (Boolean.getBoolean("reviews.preview")) {
            Path folder = Path.of("target", "review-preview"); Files.createDirectories(folder);
            Files.writeString(folder.resolve(name), html, StandardCharsets.UTF_8);
        }
        return Jsoup.parse(html);
    }
    @Controller static class Preview {
        static Review review() {
            var user = new User(); user.setFullName("Khách kiểm thử"); user.setEmail("customer@example.com");
            var product = new Product(); product.setId(1L); product.setName("Nấm kiểm thử");
            var order = new Order(); order.setId(1L); order.setOrderCode("VS-TEST-19");
            var purchase = new OrderDetail(); purchase.setId(10L); purchase.setOrder(order); purchase.setProduct(product); purchase.setProductName(product.getName()); purchase.setQuantity(2);
            var review = new Review(); review.setId(1L); review.setUser(user); review.setProduct(product); review.setRating(5); review.setComment("<script>unsafe</script>"); review.setOrderDetail(purchase);
            var image = new ReviewImage(); image.setId(1L); image.setStorageKey("test.jpg"); review.addImage(image);
            return review;
        }
        @GetMapping("/__test/phase19/product") String product(Model model) {
            new ProductTemplateRenderingTests.ProductPreviewController().preview(1L, model);
            var r = review();
            model.addAttribute("reviews", List.of(r)); model.addAttribute("ownReviews", List.of(r));
            model.addAttribute("canReview", true); model.addAttribute("eligiblePurchases", List.of(r.getOrderDetail()));
            model.addAttribute("reviewSummary", new ReviewSummary(5,1,Map.of(5,1L)));
            return "shop-detail";
        }
        @GetMapping("/__test/phase19/admin") String admin(Model model) {
            model.addAttribute("reviewPage", new PageImpl<>(List.of(review()),PageRequest.of(0,20),1));
            model.addAttribute("keyword", ""); model.addAttribute("selectedStatus", null); model.addAttribute("statuses", ReviewStatus.values());
            return "admin/reviews";
        }
        @GetMapping("/__test/phase19/detail") String detail(Model model) {
            model.addAttribute("review", review()); return "admin/review-detail";
        }
    }
}
