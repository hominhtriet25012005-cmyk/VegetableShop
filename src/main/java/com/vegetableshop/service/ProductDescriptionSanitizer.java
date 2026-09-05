package com.vegetableshop.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * Keeps the small set of formatting elements supported by the Admin editor
 * while removing scripts, event handlers, embedded media and unsafe URLs.
 */
public final class ProductDescriptionSanitizer {

    private static final Safelist PRODUCT_DESCRIPTION = Safelist.basic()
        .addTags("h2", "h3", "h4", "p", "pre")
        .addProtocols("a", "href", "http", "https", "mailto")
        .preserveRelativeLinks(true);

    private static final Document.OutputSettings OUTPUT_SETTINGS = new Document.OutputSettings()
        .prettyPrint(false);

    private ProductDescriptionSanitizer() {
    }

    public static String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }

        String safeHtml = Jsoup.clean(html.trim(), "", PRODUCT_DESCRIPTION, OUTPUT_SETTINGS).trim();
        return safeHtml.isBlank() ? null : safeHtml;
    }
}
