package com.vegetableshop.controller;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("template")
@Import({HomeTemplateRenderingTests.HomePreviewController.class,
    ProductTemplateRenderingTests.ProductPreviewController.class})
class ProductCardRenderingTests {
    @Autowired private MockMvc mockMvc;

    @Test
    @WithMockUser
    void sharedCardsKeepLinksCartAndIconOnlyWishlistWithoutDescriptions() throws Exception {
        for (String page : List.of("/__test/home", "/__test/shop-pagination", "/__test/product/1")) {
            Document document = render(page);
            var cards = document.select("[data-product-card]");
            assertFalse(cards.isEmpty(), page);
            for (Element card : cards) {
                assertFalse(card.text().contains("Xem chi tiết"));
                assertTrue(card.select(".product-description, .product-description-clamp, [data-wishlist-label]").isEmpty());
                assertEquals(card.selectFirst(".product-card-image").attr("href"),
                    card.selectFirst(".product-card-title a").attr("href"));
                assertTrue(card.selectFirst(".product-card-price").text().contains("₫ / "));
                Element favorite = card.selectFirst("button[data-wishlist-toggle]");
                assertNotNull(favorite);
                assertTrue(favorite.text().isBlank());
                assertFalse(favorite.attr("aria-label").isBlank());
                assertEquals(favorite.attr("aria-label"), favorite.attr("title"));
                assertTrue(favorite.select("i.fa-heart").size() == 1);
                Element form = card.selectFirst("form[data-cart-add]");
                assertEquals("/cart/items", form.attr("action"));
                assertEquals("post", form.attr("method"));
                assertFalse(form.selectFirst("input[name=productId]").val().isBlank());
                assertEquals("1", form.selectFirst("input[name=quantity]").val());
                assertNotNull(form.selectFirst("input[name=_csrf]"));
                assertTrue(card.select("a button, a form").isEmpty(), "Actions must not be inside the product link");
                assertEquals(card.selectFirst(".product-card-stock").text().equals("Hết hàng"),
                    form.selectFirst("button").hasAttr("disabled"));
            }
            if (page.contains("product/")) {
                assertEquals(4, cards.size(), "Related, brand, recommended and viewed products use the same card");
                assertTrue(document.select(".product-description").text().contains("Mô tả sản phẩm kiểm thử"));
                assertEquals("true", cards.getFirst().selectFirst("[data-wishlist-toggle]").attr("aria-pressed"));
            }
            exportPreview(page.contains("home") ? "index.html" : page.contains("pagination") ? "shop.html" : "detail.html", document);
        }
    }

    @Test
    void guestsSeeLoginHeartsAndCannotAddSoldOutProducts() throws Exception {
        Document document = render("/__test/home");
        for (Element card : document.select("[data-product-card]")) {
            assertTrue(card.select("[data-wishlist-toggle], form[data-cart-add]").isEmpty());
            assertEquals("/login", card.selectFirst("a.product-card-favorite").attr("href"));
            if (card.selectFirst(".product-card-stock").text().equals("Hết hàng")) {
                assertNotNull(card.selectFirst(".product-card-actions button[disabled]"));
                assertTrue(card.select(".product-card-actions a").isEmpty());
            } else {
                assertEquals("/login", card.selectFirst(".product-card-actions a").attr("href"));
            }
        }
        exportPreview("guest.html", document);
    }

    private Document render(String page) throws Exception {
        String html = mockMvc.perform(get(page)).andExpect(status().isOk()).andReturn()
            .getResponse().getContentAsString(StandardCharsets.UTF_8);
        return Jsoup.parse(html);
    }

    // Opt-in local visual fixtures only; no production data or database needed.
    private void exportPreview(String name, Document document) throws Exception {
        if (Boolean.getBoolean("cards.preview")) {
            Path folder = Path.of("target", "card-preview");
            Files.createDirectories(folder);
            Files.writeString(folder.resolve(name), document.outerHtml(), StandardCharsets.UTF_8);
        }
    }
}
