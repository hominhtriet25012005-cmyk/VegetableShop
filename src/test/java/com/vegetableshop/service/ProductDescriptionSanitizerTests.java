package com.vegetableshop.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductDescriptionSanitizerTests {

    @Test
    void keepsUsefulFormattingAndRemovesExecutableContent() {
        String result = ProductDescriptionSanitizer.sanitize(
            "<h3>Rau sạch</h3><p onclick=\"steal()\">Tươi <strong>mỗi ngày</strong></p>"
                + "<script>alert(1)</script><img src=\"https://tracker.invalid/pixel\">"
        );

        assertTrue(result.contains("<h3>Rau sạch</h3>"));
        assertTrue(result.contains("<strong>mỗi ngày</strong>"));
        assertFalse(result.contains("onclick"));
        assertFalse(result.contains("script"));
        assertFalse(result.contains("img"));
    }

    @Test
    void rejectsJavascriptUrlsAndNormalizesBlankInput() {
        String result = ProductDescriptionSanitizer.sanitize(
            "<a href=\"javascript:alert(1)\">Nguy hiểm</a>");
        assertTrue(result.contains("Nguy hiểm"));
        assertFalse(result.contains("href"));
        assertFalse(result.contains("javascript"));
        assertNull(ProductDescriptionSanitizer.sanitize("   "));
    }
}
