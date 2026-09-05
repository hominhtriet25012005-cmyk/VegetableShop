package com.vegetableshop.service;

import com.vegetableshop.dto.ChatbotProductView;
import com.vegetableshop.dto.ChatbotResponse;
import com.vegetableshop.entity.Product;
import com.vegetableshop.repository.ProductRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Profile("mysql")
public class ChatbotService {

    private static final int PRODUCT_LIMIT = 5;
    private static final int CANDIDATE_LIMIT = 80;
    private static final String NUMBER = "([0-9]+(?:[.,][0-9]+)?)\\s*(k|nghin|ngan|trieu)?";
    private static final Pattern BETWEEN_PRICE = Pattern.compile("tu\\s+" + NUMBER + "\\s+den\\s+" + NUMBER);
    private static final Pattern BOUND_PRICE = Pattern.compile(
        "(duoi|toi da|khong qua|nho hon|tren|toi thieu|it nhat|tu)\\s+" + NUMBER
    );
    private static final Set<String> STOP_WORDS = Set.of(
        "toi", "minh", "ban", "cho", "hoi", "muon", "can", "giup", "tim", "kiem", "mua",
        "co", "khong", "la", "nao", "nhung", "cac", "loai", "mot", "vai", "san", "pham",
        "gia", "duoi", "tren", "tu", "den", "da", "nho", "hon", "it", "nhat",
        "nghin", "ngan", "trieu", "dong", "vnd", "k", "de", "voi", "va", "hoac",
        "goi", "y", "re", "thap", "xem"
    );

    private final ProductRepository productRepository;

    public ChatbotService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public ChatbotResponse reply(String rawMessage) {
        String normalized = normalize(rawMessage);
        ChatbotResponse faq = faqResponse(normalized);
        if (faq != null) {
            return faq;
        }

        PriceRange priceRange = extractPriceRange(rawMessage);
        List<String> terms = searchTerms(normalized);
        boolean generalProductRequest = containsAny(normalized,
            "san pham", "goi y", "gia re", "re nhat", "mua gi", "dang ban");

        List<ScoredProduct> matches = productRepository
            .findChatbotCandidates(0, PageRequest.of(0, CANDIDATE_LIMIT))
            .stream()
            .filter(product -> priceRange.includes(product.getPrice()))
            .map(product -> new ScoredProduct(product, score(product, terms)))
            .filter(item -> terms.isEmpty() || item.score() > 0)
            .sorted(productComparator(normalized))
            .limit(PRODUCT_LIMIT)
            .toList();

        if (matches.isEmpty()) {
            return ChatbotResponse.message(
                "Mình chưa tìm thấy sản phẩm đang bán và còn hàng phù hợp. Bạn thử nhập tên ngắn hơn, danh mục hoặc mức giá khác nhé.",
                defaultSuggestions()
            );
        }

        if (!generalProductRequest && terms.isEmpty() && priceRange.isEmpty()) {
            return fallbackResponse();
        }

        List<ChatbotProductView> products = matches.stream()
            .map(item -> toView(item.product()))
            .toList();
        return new ChatbotResponse(
            "Mình tìm thấy " + products.size() + " sản phẩm đang còn hàng phù hợp với yêu cầu của bạn.",
            products,
            List.of("Tìm sản phẩm dưới 100.000đ", "Phí giao hàng", "Xem đơn hàng của tôi")
        );
    }

    private ChatbotResponse faqResponse(String message) {
        if (message.isBlank()) {
            return fallbackResponse();
        }
        if (containsAny(message, "xin chao", "chao ban", "hello", "hi chatbot", "chatbot oi")) {
            return ChatbotResponse.message(
                "Xin chào! Mình có thể giúp bạn tìm sản phẩm, xem chính sách giao hàng, thanh toán và hướng dẫn kiểm tra đơn hàng.",
                defaultSuggestions()
            );
        }
        if (containsAny(message, "giao hang", "van chuyen", "phi ship", "ship hang")) {
            return ChatbotResponse.message(
                "Phí và thời gian giao hàng được xác nhận theo địa chỉ lúc checkout. Bạn có thể xem tổng tiền trước khi đặt đơn COD.",
                List.of("Tìm rau củ", "Thanh toán thế nào?", "Liên hệ cửa hàng")
            );
        }
        if (containsAny(message, "thanh toan", "cod", "tra tien")) {
            return ChatbotResponse.message(
                "Hiện cửa hàng hỗ trợ thanh toán COD. Tổng tiền được hệ thống tính lại từ giá và tồn kho trước khi tạo đơn.",
                List.of("Tìm sản phẩm", "Phí giao hàng", "Xem đơn hàng của tôi")
            );
        }
        if (containsAny(message, "doi tra", "hoan hang", "tra hang", "huy don")) {
            return ChatbotResponse.message(
                "Nếu cần đổi trả hoặc hủy đơn, bạn hãy mở Đơn hàng của tôi để xem trạng thái rồi liên hệ cửa hàng. Đơn đã giao cần được nhân viên hỗ trợ xác minh.",
                List.of("Xem đơn hàng của tôi", "Liên hệ cửa hàng")
            );
        }
        if (containsAny(message, "don hang", "theo doi don", "kiem tra don")) {
            return ChatbotResponse.message(
                "Bạn đăng nhập rồi mở mục Đơn hàng của tôi để xem trạng thái và chi tiết. Chatbot không đọc hoặc hiển thị dữ liệu đơn hàng riêng tư.",
                List.of("Mở Đơn hàng của tôi", "Liên hệ cửa hàng")
            );
        }
        if (containsAny(message, "dang nhap", "dang ky", "quen mat khau", "tai khoan")) {
            return ChatbotResponse.message(
                "Bạn có thể đăng ký, đăng nhập hoặc dùng chức năng Quên mật khẩu trong menu tài khoản. Không gửi mật khẩu vào chatbot nhé.",
                List.of("Đăng nhập", "Đăng ký tài khoản", "Quên mật khẩu")
            );
        }
        if (containsAny(message, "lien he", "hotline", "dia chi", "email")) {
            return ChatbotResponse.message(
                "Bạn có thể dùng trang Liên hệ, email support@vegetableshop.vn hoặc số 0123 456 789 để được nhân viên hỗ trợ.",
                List.of("Mở trang Liên hệ", "Tìm sản phẩm")
            );
        }
        return null;
    }

    private int score(Product product, List<String> terms) {
        if (terms.isEmpty()) {
            return 1;
        }
        String name = normalize(product.getName());
        String category = normalize(product.getCategory().getName());
        String brand = product.getBrand() == null ? "" : normalize(product.getBrand().getName());
        String description = normalize(product.getDescription());
        int score = 0;
        for (String term : terms) {
            if (name.contains(term)) score += 8;
            if (category.contains(term)) score += 5;
            if (brand.contains(term)) score += 3;
            if (description.contains(term)) score += 1;
        }
        return score;
    }

    private Comparator<ScoredProduct> productComparator(String message) {
        Comparator<ScoredProduct> byScore = Comparator.comparingInt(ScoredProduct::score).reversed();
        Comparator<ScoredProduct> byPrice = Comparator.comparing(item -> item.product().getPrice());
        if (containsAny(message, "gia re", "re nhat", "thap nhat")) {
            return byPrice.thenComparing(byScore).thenComparing(item -> item.product().getId());
        }
        return byScore.thenComparing(byPrice).thenComparing(item -> item.product().getId());
    }

    ChatbotProductView toView(Product product) {
        String unit = product.getUnit() == null ? "sản phẩm" : product.getUnit().getSymbol();
        String image = product.getImage() == null || product.getImage().isBlank()
            ? "/img/hero-img.jpg"
            : product.getImage().trim();
        return new ChatbotProductView(
            product.getId(), product.getName(), product.getPrice(), image,
            product.getCategory().getName(), unit, product.getQuantity(),
            "/product/" + product.getId()
        );
    }

    private PriceRange extractPriceRange(String rawMessage) {
        String priceText = normalizeForPrice(rawMessage);
        Matcher between = BETWEEN_PRICE.matcher(priceText);
        if (between.find()) {
            return new PriceRange(
                parseMoney(between.group(1), between.group(2)),
                parseMoney(between.group(3), between.group(4))
            );
        }

        BigDecimal minimum = null;
        BigDecimal maximum = null;
        Matcher bound = BOUND_PRICE.matcher(priceText);
        while (bound.find()) {
            BigDecimal amount = parseMoney(bound.group(2), bound.group(3));
            switch (bound.group(1)) {
                case "duoi", "toi da", "khong qua", "nho hon" -> maximum = amount;
                default -> minimum = amount;
            }
        }
        return new PriceRange(minimum, maximum);
    }

    private BigDecimal parseMoney(String number, String unit) {
        BigDecimal value;
        if (unit != null && !unit.isBlank()) {
            value = new BigDecimal(number.replace(',', '.'));
            if (unit.equals("trieu")) {
                return value.multiply(BigDecimal.valueOf(1_000_000));
            }
            return value.multiply(BigDecimal.valueOf(1_000));
        }
        return new BigDecimal(number.replace(".", "").replace(",", ""));
    }

    private List<String> searchTerms(String normalized) {
        Set<String> terms = new HashSet<>();
        for (String token : normalized.split("\\s+")) {
            boolean priceToken = token.matches("[0-9]+(?:k|d)?");
            if (token.length() >= 2 && !STOP_WORDS.contains(token) && !priceToken) {
                terms.add(token);
            }
        }
        return new ArrayList<>(terms);
    }

    private ChatbotResponse fallbackResponse() {
        return ChatbotResponse.message(
            "Mình chưa hiểu rõ câu hỏi. Bạn có thể hỏi về sản phẩm, mức giá, giao hàng, thanh toán hoặc đơn hàng.",
            defaultSuggestions()
        );
    }

    private List<String> defaultSuggestions() {
        return List.of("Tìm nấm dưới 100.000đ", "Phí giao hàng", "Thanh toán thế nào?", "Xem đơn hàng của tôi");
    }

    private boolean containsAny(String value, String... phrases) {
        for (String phrase : phrases) {
            if (value.contains(phrase)) return true;
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) return "";
        return normalizeForPrice(value).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private String normalizeForPrice(String value) {
        if (value == null) return "";
        String decomposed = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").replace('đ', 'd');
    }

    private record ScoredProduct(Product product, int score) {
    }

    private record PriceRange(BigDecimal minimum, BigDecimal maximum) {
        boolean includes(BigDecimal price) {
            return (minimum == null || price.compareTo(minimum) >= 0)
                && (maximum == null || price.compareTo(maximum) <= 0);
        }

        boolean isEmpty() {
            return minimum == null && maximum == null;
        }
    }
}
