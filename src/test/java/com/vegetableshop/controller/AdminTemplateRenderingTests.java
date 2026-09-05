package com.vegetableshop.controller;

import com.vegetableshop.dto.AdminDashboardView;
import com.vegetableshop.dto.AdminProductRequest;
import com.vegetableshop.dto.AdminReportView;
import com.vegetableshop.dto.AdminChatbotFaqRequest;
import com.vegetableshop.dto.ChatbotAdminDashboard;
import com.vegetableshop.entity.Category;
import com.vegetableshop.entity.Brand;
import com.vegetableshop.entity.InventoryDocument;
import com.vegetableshop.entity.InventoryDocumentItem;
import com.vegetableshop.entity.InventoryDocumentStatus;
import com.vegetableshop.entity.InventoryDocumentType;
import com.vegetableshop.entity.Order;
import com.vegetableshop.entity.OrderStatus;
import com.vegetableshop.entity.PaymentStatus;
import com.vegetableshop.entity.PaymentMethod;
import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.ProductUnit;
import com.vegetableshop.entity.Role;
import com.vegetableshop.entity.User;
import com.vegetableshop.entity.StockMovement;
import com.vegetableshop.entity.StockMovementType;
import com.vegetableshop.entity.Supplier;
import com.vegetableshop.entity.ChatbotFaq;
import com.vegetableshop.entity.ChatbotInteraction;
import com.vegetableshop.entity.ChatbotInteractionStatus;
import com.vegetableshop.entity.ChatbotResponseSource;
import com.vegetableshop.dto.InventoryOverviewView;
import com.vegetableshop.dto.InventoryDocumentRequest;
import com.vegetableshop.dto.InventoryMovementRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("template")
@Import(AdminTemplateRenderingTests.AdminPreviewController.class)
class AdminTemplateRenderingTests {

    @Autowired private MockMvc mockMvc;

    @Test
    void dashboardRendersDatabaseMetricsRecentOrderAndLowStock() throws Exception {
        mockMvc.perform(get("/__test/admin-dashboard").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("250.000 ₫")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("VS-ADMIN-001")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Cà rốt sắp hết")));
    }

    @Test
    void productAndUserTablesRenderDataWithoutPassword() throws Exception {
        mockMvc.perform(get("/__test/admin-products").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Cam kiểm thử")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Tất cả danh mục")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Tất cả thương hiệu")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Tất cả trạng thái")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("data-confirm")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("sweetalert2@11")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Quay về website")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Giai đoạn 8"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Md. Hasan Mahmud"))))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        mockMvc.perform(get("/__test/admin-users").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("user@example.com")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("bcrypt-secret"))));
    }

    @Test
    void productFormContainsBackendValidationAndCsrf() throws Exception {
        mockMvc.perform(get("/__test/admin-product-form").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Lưu sản phẩm")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("data-rich-text-editor")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("summernote-lite.min.js")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("/admin/js/product-editor.js")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    @Test
    void categoriesAndOrderDetailRenderAdminActions() throws Exception {
        mockMvc.perform(get("/__test/admin-categories").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Rau củ")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        mockMvc.perform(get("/__test/admin-order-detail").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("VS-ADMIN-001")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("CONFIRMED")));
    }

    @Test
    void businessReportRendersChartsFiltersInventoryAndSales() throws Exception {
        mockMvc.perform(get("/__test/admin-reports").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Thống kê kinh doanh")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("310.000 ₫")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Nấm hương")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("monthlyRevenueChart")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("export.csv")));
    }

    @Test
    void inventoryPagesRenderActionsWarningsAndImmutableHistory() throws Exception {
        mockMvc.perform(get("/__test/admin-inventory").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Quản lý kho")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Tạo phiếu kho")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Danh sách phiếu")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Ngưỡng cảnh báo")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));

        mockMvc.perform(get("/__test/admin-inventory-documents").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Phiếu kho của Nấm hương")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("PN-20260830-000001")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Nhập mới sản phẩm này")));

        mockMvc.perform(get("/__test/admin-inventory-history").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Lịch sử biến động kho")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Nhập hàng kiểm thử")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("admin@example.com")));

        mockMvc.perform(get("/__test/admin-inventory-document-form").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Lưu bản nháp")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Nhà cung cấp")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("/admin/js/inventory-document.js")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));

        mockMvc.perform(get("/__test/admin-inventory-document-detail").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("PN-20260830-000001")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Xác nhận và cập nhật kho")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    @Test
    void chatbotDashboardFaqListAndFormRenderPrivacyControls() throws Exception {
        mockMvc.perform(get("/__test/admin-chatbot").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Chatbot &amp; thống kê hội thoại")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Không lưu tài khoản, session hoặc API key")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Câu hỏi đã làm sạch")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));

        mockMvc.perform(get("/__test/admin-chatbot-faqs").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Phí giao hàng được tính như thế nào?")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("FAQ chính thức")));

        mockMvc.perform(get("/__test/admin-chatbot-faq-form").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Câu trả lời chính thức")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Lưu FAQ")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    @Controller
    static class AdminPreviewController {

        @GetMapping("/__test/admin-dashboard")
        String dashboard(Model model) {
            Product lowStock = product(3L, "Cà rốt sắp hết", 4);
            Order order = order();
            model.addAttribute("dashboard", new AdminDashboardView(
                5, 3, 2, 1, new BigDecimal("250000"), List.of(order), List.of(lowStock)
            ));
            return "admin/dashboard";
        }

        @GetMapping("/__test/admin-products")
        String products(Model model) {
            Product product = product(4L, "Cam kiểm thử", 12);
            model.addAttribute("productPage", new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1));
            model.addAttribute("keyword", "");
            model.addAttribute("categories", List.of(category()));
            model.addAttribute("brands", List.of(brand()));
            model.addAttribute("selectedCategoryId", 1L);
            model.addAttribute("selectedBrandId", 1L);
            model.addAttribute("selectedStatus", true);
            return "admin/products";
        }

        @GetMapping("/__test/admin-users")
        String users(Model model) {
            User user = new User();
            user.setId(7L);
            user.setFullName("Người dùng kiểm thử");
            user.setEmail("user@example.com");
            user.setPassword("bcrypt-secret");
            user.setRole(Role.USER);
            user.setStatus(true);
            model.addAttribute("userPage", new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1));
            model.addAttribute("keyword", "");
            return "admin/users";
        }

        @GetMapping("/__test/admin-product-form")
        String productForm(Model model) {
            Category category = category();
            AdminProductRequest request = new AdminProductRequest();
            request.setSku("RAU-CU-001");
            request.setCategoryId(category.getId());
            model.addAttribute("productRequest", request);
            model.addAttribute("categories", List.of(category));
            model.addAttribute("brands", List.of(brand()));
            model.addAttribute("productUnits", ProductUnit.values());
            model.addAttribute("productId", null);
            return "admin/product-form";
        }

        @GetMapping("/__test/admin-categories")
        String categories(Model model) {
            model.addAttribute("categories", List.of(category()));
            return "admin/categories";
        }

        @GetMapping("/__test/admin-order-detail")
        String orderDetail(Model model) {
            model.addAttribute("order", order());
            model.addAttribute("allowedStatuses", List.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
            return "admin/order-detail";
        }

        @GetMapping("/__test/admin-reports")
        String reports(Model model) {
            var sales = List.of(new AdminReportView.SalesRow(
                "Nấm hương", 3, new BigDecimal("150000")));
            var inventory = List.of(new AdminReportView.InventoryRow(
                1L, "Nấm hương", "Nấm các loại", "Nông trại A", 5, 10, true));
            model.addAttribute("report", new AdminReportView(
                java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2026, 12, 31),
                new BigDecimal("310000"), 2, 6, new BigDecimal("155000"),
                5, 1, 0, 10, inventory, sales, sales, sales, sales, sales,
                List.of(new AdminReportView.RevenuePoint("2026-01", new BigDecimal("310000"))),
                List.of(new AdminReportView.RevenuePoint("2026-Q1", new BigDecimal("310000"))),
                List.of(new AdminReportView.RevenuePoint("2026", new BigDecimal("310000")))
            ));
            return "admin/reports";
        }

        @GetMapping("/__test/admin-inventory")
        String inventory(Model model) {
            model.addAttribute("inventory", new InventoryOverviewView(
                5, 1, 0, List.of(new InventoryOverviewView.ProductRow(
                    1L, "Nấm hương", "Nấm các loại", "Nông trại A", 5, 10, true))
            ));
            model.addAttribute("keyword", "");
            model.addAttribute("selectedStockStatus", "ALL");
            return "admin/inventory";
        }

        @GetMapping("/__test/admin-inventory-history")
        String inventoryHistory(Model model) {
            Product product = product(1L, "Nấm hương", 15);
            StockMovement movement = new StockMovement();
            movement.setId(1L);
            movement.setProduct(product);
            movement.setProductName(product.getName());
            movement.setMovementType(StockMovementType.INBOUND);
            movement.setQuantityBefore(10);
            movement.setQuantityChange(5);
            movement.setQuantityAfter(15);
            movement.setReason("Nhập hàng kiểm thử");
            movement.setPerformedBy("admin@example.com");
            ReflectionTestUtils.setField(movement, "createdAt", LocalDateTime.of(2026, 8, 30, 10, 0));
            model.addAttribute("movementPage", new PageImpl<>(List.of(movement), PageRequest.of(0, 20), 1));
            model.addAttribute("products", List.of(product));
            model.addAttribute("movementTypes", StockMovementType.values());
            model.addAttribute("selectedProductId", null);
            model.addAttribute("selectedMovementType", null);
            model.addAttribute("fromDate", null);
            model.addAttribute("toDate", null);
            return "admin/inventory-history";
        }

        @GetMapping("/__test/admin-inventory-document-form")
        String inventoryDocumentForm(Model model) {
            InventoryDocumentRequest request = new InventoryDocumentRequest();
            request.setType(InventoryDocumentType.INBOUND);
            model.addAttribute("documentRequest", request);
            model.addAttribute("documentId", null);
            model.addAttribute("products", List.of(product(1L, "Nấm hương", 15)));
            model.addAttribute("suppliers", List.of(supplier()));
            model.addAttribute("documentTypes", InventoryDocumentType.values());
            return "admin/inventory-document-form";
        }

        @GetMapping("/__test/admin-inventory-documents")
        String inventoryDocuments(Model model) {
            Product selectedProduct = product(1L, "Nấm hương", 15);
            model.addAttribute("documentPage", new PageImpl<>(
                List.of(inventoryDocument()), PageRequest.of(0, 15), 1));
            model.addAttribute("products", List.of(selectedProduct));
            model.addAttribute("suppliers", List.of(supplier()));
            model.addAttribute("documentTypes", InventoryDocumentType.values());
            model.addAttribute("documentStatuses", InventoryDocumentStatus.values());
            model.addAttribute("keyword", "");
            model.addAttribute("selectedType", null);
            model.addAttribute("selectedStatus", null);
            model.addAttribute("selectedSupplierId", null);
            model.addAttribute("selectedProductId", selectedProduct.getId());
            model.addAttribute("selectedProduct", selectedProduct);
            return "admin/inventory-documents";
        }

        @GetMapping("/__test/admin-inventory-document-detail")
        String inventoryDocumentDetail(Model model) {
            model.addAttribute("document", inventoryDocument());
            return "admin/inventory-document-detail";
        }

        @GetMapping("/__test/admin-chatbot")
        String chatbotDashboard(Model model) {
            LocalDate today = LocalDate.of(2026, 8, 30);
            ChatbotInteraction interaction = new ChatbotInteraction();
            interaction.setQuestion("Phí giao hàng là bao nhiêu?");
            interaction.setResponseSource(ChatbotResponseSource.MANAGED_FAQ);
            interaction.setStatus(ChatbotInteractionStatus.RESOLVED);
            interaction.setProductCount(0);
            interaction.setResponseTimeMs(12L);
            interaction.setCreatedAt(today.atTime(10, 30));
            model.addAttribute("chatbotDashboard", new ChatbotAdminDashboard(
                today, today, 1, 0, 1, 0, 0, 12, 5,
                List.of(new ChatbotAdminDashboard.DailyCount(today, 1, 100)),
                new PageImpl<>(List.of(interaction), PageRequest.of(0, 15), 1)
            ));
            model.addAttribute("keyword", "");
            model.addAttribute("selectedSource", null);
            model.addAttribute("selectedStatus", null);
            model.addAttribute("sources", ChatbotResponseSource.values());
            model.addAttribute("interactionStatuses", ChatbotInteractionStatus.values());
            model.addAttribute("retentionDays", 30);
            return "admin/chatbot-dashboard";
        }

        @GetMapping("/__test/admin-chatbot-faqs")
        String chatbotFaqs(Model model) {
            model.addAttribute("faqs", List.of(chatbotFaq()));
            model.addAttribute("keyword", "");
            return "admin/chatbot-faqs";
        }

        @GetMapping("/__test/admin-chatbot-faq-form")
        String chatbotFaqForm(Model model) {
            model.addAttribute("faqRequest", AdminChatbotFaqRequest.from(chatbotFaq()));
            model.addAttribute("faqId", null);
            return "admin/chatbot-faq-form";
        }

        private static Product product(Long id, String name, int stock) {
            Product product = new Product();
            product.setId(id);
            product.setName(name);
            product.setPrice(new BigDecimal("35000"));
            product.setQuantity(stock);
            product.setSku("SP-" + id);
            product.setUnit(ProductUnit.KILOGRAM);
            product.setImage("/img/vegetable-item-1.jpg");
            product.setStatus(true);
            product.setCategory(category());
            return product;
        }

        private static Brand brand() {
            Brand brand = new Brand();
            brand.setId(1L);
            brand.setName("Vegetable Shop");
            brand.setStatus(true);
            return brand;
        }

        private static Supplier supplier() {
            Supplier supplier = new Supplier();
            supplier.setId(1L);
            supplier.setCode("NCC-001");
            supplier.setName("Nông trại A");
            supplier.setStatus(true);
            return supplier;
        }

        private static ChatbotFaq chatbotFaq() {
            ChatbotFaq faq = new ChatbotFaq();
            faq.setId(1L);
            faq.setQuestion("Phí giao hàng được tính như thế nào?");
            faq.setAnswer("Phí được hiển thị ở bước thanh toán.");
            faq.setKeywords("phí giao hàng, phí ship");
            faq.setDisplayOrder(10);
            faq.setStatus(true);
            faq.setCreatedBy("admin@example.com");
            faq.setUpdatedBy("admin@example.com");
            return faq;
        }

        private static InventoryDocument inventoryDocument() {
            Product product = product(1L, "Nấm hương", 15);
            InventoryDocument document = new InventoryDocument();
            document.setId(1L);
            document.setCode("PN-20260830-000001");
            document.setType(InventoryDocumentType.INBOUND);
            document.setStatus(InventoryDocumentStatus.DRAFT);
            document.setSupplier(supplier());
            document.setCreatedBy("admin@example.com");
            ReflectionTestUtils.setField(document, "createdAt", LocalDateTime.of(2026, 8, 30, 10, 0));
            InventoryDocumentItem item = new InventoryDocumentItem();
            item.setProduct(product);
            item.setProductSku(product.getSku());
            item.setProductName(product.getName());
            item.setQuantity(5);
            item.setUnitCost(new BigDecimal("22000"));
            document.addItem(item);
            return document;
        }

        private static Category category() {
            Category category = new Category();
            category.setId(1L);
            category.setName("Rau củ");
            category.setStatus(true);
            return category;
        }

        private static Order order() {
            User user = new User();
            user.setFullName("Khách kiểm thử");
            Order order = new Order();
            order.setId(1L);
            order.setOrderCode("VS-ADMIN-001");
            order.setUser(user);
            order.setStatus(OrderStatus.PENDING);
            order.setPaymentMethod(PaymentMethod.COD);
            order.setPaymentStatus(PaymentStatus.UNPAID);
            order.setReceiverName("Khách kiểm thử");
            order.setReceiverPhone("0901234567");
            order.setShippingAddress("123 Nguyễn Trãi");
            order.setTotalAmount(new BigDecimal("250000"));
            ReflectionTestUtils.setField(order, "createdAt", LocalDateTime.of(2026, 8, 27, 20, 0));
            return order;
        }
    }
}
