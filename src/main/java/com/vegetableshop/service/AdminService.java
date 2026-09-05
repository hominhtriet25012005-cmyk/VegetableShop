package com.vegetableshop.service;

import com.vegetableshop.dto.AdminCategoryRequest;
import com.vegetableshop.dto.AdminDashboardView;
import com.vegetableshop.dto.AdminProductRequest;
import com.vegetableshop.dto.AdminReportView;
import com.vegetableshop.entity.Category;
import com.vegetableshop.entity.Brand;
import com.vegetableshop.entity.Order;
import com.vegetableshop.entity.OrderDetail;
import com.vegetableshop.entity.OrderStatus;
import com.vegetableshop.entity.PaymentMethod;
import com.vegetableshop.entity.PaymentStatus;
import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.Role;
import com.vegetableshop.entity.User;
import com.vegetableshop.entity.Supplier;
import com.vegetableshop.entity.ProductUnit;
import com.vegetableshop.event.OrderStatusChangedMailEvent;
import com.vegetableshop.exception.AdminOperationException;
import com.vegetableshop.exception.OrderNotFoundException;
import com.vegetableshop.exception.ProductNotFoundException;
import com.vegetableshop.repository.CategoryRepository;
import com.vegetableshop.repository.BrandRepository;
import com.vegetableshop.repository.OrderRepository;
import com.vegetableshop.repository.AdminProductSpecifications;
import com.vegetableshop.repository.ProductRepository;
import com.vegetableshop.repository.UserRepository;
import com.vegetableshop.repository.SupplierRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Service
@Profile("mysql")
public class AdminService {
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private VoucherService voucherService;

    private static final int PAGE_SIZE = 10;
    private static final int LOW_STOCK_LIMIT = 10;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final SupplierRepository supplierRepository;
    private final BrandRepository brandRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final InventoryService inventoryService;
    private final InventoryDocumentService inventoryDocumentService;

    public AdminService(
        ProductRepository productRepository,
        CategoryRepository categoryRepository,
        OrderRepository orderRepository,
        UserRepository userRepository,
        SupplierRepository supplierRepository,
        BrandRepository brandRepository,
        ApplicationEventPublisher eventPublisher,
        InventoryService inventoryService,
        InventoryDocumentService inventoryDocumentService
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.supplierRepository = supplierRepository;
        this.brandRepository = brandRepository;
        this.eventPublisher = eventPublisher;
        this.inventoryService = inventoryService;
        this.inventoryDocumentService = inventoryDocumentService;
    }

    @Transactional(readOnly = true)
    public AdminDashboardView dashboard() {
        return new AdminDashboardView(
            productRepository.count(),
            categoryRepository.count(),
            userRepository.count(),
            orderRepository.count(),
            orderRepository.calculateRevenueByStatus(OrderStatus.COMPLETED),
            orderRepository.findTop5ByOrderByCreatedAtDesc(),
            productRepository.findLowStockProducts(PageRequest.of(0, 5))
        );
    }

    @Transactional(readOnly = true)
    public AdminReportView businessReport(LocalDate requestedFrom, LocalDate requestedTo) {
        LocalDate today = LocalDate.now();
        LocalDate fromDate = requestedFrom == null ? today.withDayOfYear(1) : requestedFrom;
        LocalDate toDate = requestedTo == null ? today : requestedTo;
        if (fromDate.isAfter(toDate)) {
            throw new AdminOperationException("Ngày bắt đầu không được sau ngày kết thúc");
        }

        List<Order> completedOrders = orderRepository.findCompletedForReport(
            fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay());
        List<Product> products = productRepository.findAllByOrderByQuantityAscNameAsc();

        Map<String, MutableSales> productSales = new LinkedHashMap<>();
        Map<String, MutableSales> categorySales = new LinkedHashMap<>();
        Map<String, MutableSales> brandSales = new LinkedHashMap<>();
        Map<String, MutableSales> customerSales = new LinkedHashMap<>();
        Map<String, BigDecimal> monthlyRevenue = new TreeMap<>();
        Map<String, BigDecimal> quarterlyRevenue = new TreeMap<>();
        Map<String, BigDecimal> yearlyRevenue = new TreeMap<>();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        long unitsSold = 0;
        for (Order order : completedOrders) {
            BigDecimal orderRevenue = money(order.getTotalAmount());
            long orderUnits = order.getDetails().stream()
                .mapToLong(detail -> detail.getQuantity() == null ? 0 : detail.getQuantity())
                .sum();
            totalRevenue = totalRevenue.add(orderRevenue);
            unitsSold += orderUnits;

            mergeSales(customerSales, customerLabel(order), orderUnits, orderRevenue);
            mergeRevenue(monthlyRevenue, YearMonth.from(order.getCreatedAt()).toString(), orderRevenue);
            int quarter = ((order.getCreatedAt().getMonthValue() - 1) / 3) + 1;
            mergeRevenue(quarterlyRevenue, order.getCreatedAt().getYear() + "-Q" + quarter, orderRevenue);
            mergeRevenue(yearlyRevenue, Integer.toString(order.getCreatedAt().getYear()), orderRevenue);

            for (OrderDetail detail : order.getDetails()) {
                long quantity = detail.getQuantity() == null ? 0 : detail.getQuantity();
                BigDecimal lineRevenue = money(detail.getSubtotal());
                Product product = detail.getProduct();
                mergeSales(productSales, fallback(detail.getProductName(), "Sản phẩm không xác định"),
                    quantity, lineRevenue);
                mergeSales(categorySales,
                    product != null && product.getCategory() != null
                        ? fallback(product.getCategory().getName(), "Chưa phân loại") : "Chưa phân loại",
                    quantity, lineRevenue);
                mergeSales(brandSales,
                    product != null && product.getBrand() != null
                        ? fallback(product.getBrand().getName(), "Không thương hiệu")
                        : "Không thương hiệu",
                    quantity, lineRevenue);
            }
        }

        List<AdminReportView.InventoryRow> inventory = products.stream()
            .map(product -> new AdminReportView.InventoryRow(
                product.getId(),
                product.getName(),
                product.getCategory() == null ? "Chưa phân loại" : product.getCategory().getName(),
                product.getBrand() == null ? "Không thương hiệu" : product.getBrand().getName(),
                product.getQuantity() == null ? 0 : product.getQuantity(),
                product.getLowStockThreshold() == null ? LOW_STOCK_LIMIT : product.getLowStockThreshold(),
                product.isStatus()
            ))
            .toList();
        long totalInventoryUnits = inventory.stream().mapToLong(AdminReportView.InventoryRow::quantity).sum();
        long lowStockCount = inventory.stream().filter(AdminReportView.InventoryRow::lowStock).count();
        long outOfStockCount = inventory.stream().filter(AdminReportView.InventoryRow::outOfStock).count();
        BigDecimal averageOrderValue = completedOrders.isEmpty() ? BigDecimal.ZERO
            : totalRevenue.divide(BigDecimal.valueOf(completedOrders.size()), 0, RoundingMode.HALF_UP);

        return new AdminReportView(
            fromDate, toDate, totalRevenue, completedOrders.size(), unitsSold, averageOrderValue,
            totalInventoryUnits, lowStockCount, outOfStockCount, LOW_STOCK_LIMIT,
            List.copyOf(inventory), salesRows(productSales), salesRows(categorySales),
            salesRows(brandSales), inventoryDocumentService.supplierPurchases(fromDate, toDate),
            salesRows(customerSales), revenuePoints(monthlyRevenue),
            revenuePoints(quarterlyRevenue), revenuePoints(yearlyRevenue)
        );
    }

    public byte[] exportBusinessReportCsv(AdminReportView report) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        appendCsvRow(csv, "BÁO CÁO KINH DOANH", report.fromDate() + " đến " + report.toDate());
        appendCsvRow(csv, "Chỉ tiêu", "Giá trị");
        appendCsvRow(csv, "Doanh thu đơn hoàn tất", report.totalRevenue().toPlainString());
        appendCsvRow(csv, "Đơn hoàn tất", Long.toString(report.completedOrderCount()));
        appendCsvRow(csv, "Sản phẩm đã bán", Long.toString(report.unitsSold()));
        appendCsvRow(csv, "Giá trị đơn trung bình", report.averageOrderValue().toPlainString());

        appendSalesSection(csv, "DOANH SỐ THEO SẢN PHẨM", report.productSales());
        appendSalesSection(csv, "DOANH SỐ THEO DANH MỤC", report.categorySales());
        appendSalesSection(csv, "DOANH SỐ THEO THƯƠNG HIỆU", report.brandSales());
        appendSalesSection(csv, "NHẬP HÀNG THEO NHÀ CUNG CẤP", report.supplierPurchases());
        appendSalesSection(csv, "DOANH SỐ THEO KHÁCH HÀNG", report.customerSales());

        csv.append('\n');
        appendCsvRow(csv, "TỒN KHO", "Danh mục", "Thương hiệu", "Số lượng", "Ngưỡng tồn thấp", "Trạng thái");
        for (AdminReportView.InventoryRow row : report.inventory()) {
            appendCsvRow(csv, row.productName(), row.categoryName(), row.brandName(),
                Integer.toString(row.quantity()), Integer.toString(row.lowStockThreshold()),
                row.active() ? "Hoạt động" : "Đã ẩn");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public Page<Product> findProducts(String keyword, Long categoryId, Long brandId, Boolean status, int page) {
        String normalized = normalizeKeyword(keyword);
        Long normalizedCategoryId = categoryId != null && categoryId > 0 ? categoryId : null;
        PageRequest pageable = PageRequest.of(normalizePage(page), PAGE_SIZE,
            Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return productRepository.findAll(
            AdminProductSpecifications.withFilters(normalized, normalizedCategoryId,
                brandId != null && brandId > 0 ? brandId : null, status),
            pageable
        );
    }

    @Transactional(readOnly = true)
    public AdminProductRequest getProductForm(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        product.getCategory().getName();
        product.getAdditionalImages().size();
        return AdminProductRequest.from(product);
    }

    @Transactional
    public Product createProduct(AdminProductRequest request) {
        return createProduct(request, "SYSTEM");
    }

    @Transactional
    public Product createProduct(AdminProductRequest request, String performedBy) {
        Product savedProduct = productRepository.save(applyProduct(new Product(), request, performedBy, true));
        inventoryService.recordInitial(savedProduct, performedBy);
        return savedProduct;
    }

    @Transactional
    public Product updateProduct(Long id, AdminProductRequest request) {
        return updateProduct(id, request, "SYSTEM");
    }

    @Transactional
    public Product updateProduct(Long id, AdminProductRequest request, String performedBy) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        return productRepository.save(applyProduct(product, request, performedBy, false));
    }

    @Transactional
    public boolean toggleProductStatus(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        product.setStatus(!product.isStatus());
        return product.isStatus();
    }

    @Transactional(readOnly = true)
    public List<Category> findCategories() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name", "id"));
    }

    @Transactional(readOnly = true)
    public List<Supplier> findSuppliers() {
        return supplierRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Brand> findBrands() {
        return brandRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public AdminCategoryRequest getCategoryForm(Long id) {
        return AdminCategoryRequest.from(findCategory(id));
    }

    @Transactional
    public Category createCategory(AdminCategoryRequest request) {
        requireUniqueCategoryName(request.getName(), null);
        return categoryRepository.save(applyCategory(new Category(), request));
    }

    @Transactional
    public Category updateCategory(Long id, AdminCategoryRequest request) {
        Category category = findCategory(id);
        requireUniqueCategoryName(request.getName(), id);
        return categoryRepository.save(applyCategory(category, request));
    }

    @Transactional
    public boolean toggleCategoryStatus(Long id) {
        Category category = findCategory(id);
        category.setStatus(!category.isStatus());
        return category.isStatus();
    }

    @Transactional(readOnly = true)
    public Page<Order> findOrders(String keyword, OrderStatus status, int page) {
        PageRequest pageable = PageRequest.of(normalizePage(page), PAGE_SIZE,
            Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return orderRepository.searchForAdmin(normalizeKeyword(keyword), status, pageable);
    }

    @Transactional(readOnly = true)
    public Order findOrder(Long id) {
        return orderRepository.findAdminById(id)
            .orElseThrow(() -> new OrderNotFoundException(id));
    }

    public List<OrderStatus> allowedTransitions(OrderStatus currentStatus) {
        return switch (currentStatus) {
            case PENDING -> List.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED);
            case CONFIRMED -> List.of(OrderStatus.SHIPPING);
            case SHIPPING -> List.of(OrderStatus.COMPLETED);
            case COMPLETED, CANCELLED -> List.of();
        };
    }

    @Transactional
    public Order updateOrderStatus(Long id, OrderStatus targetStatus) {
        return updateOrderStatus(id, targetStatus, "SYSTEM");
    }

    @Transactional
    public Order updateOrderStatus(Long id, OrderStatus targetStatus, String performedBy) {
        Order order = orderRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new OrderNotFoundException(id));
        if (targetStatus == null || !allowedTransitions(order.getStatus()).contains(targetStatus)) {
            throw new AdminOperationException(
                "Không thể chuyển đơn từ " + order.getStatus() + " sang " + targetStatus
            );
        }

        if (order.getPaymentMethod() == PaymentMethod.BANK_TRANSFER) {
            if ((targetStatus == OrderStatus.SHIPPING || targetStatus == OrderStatus.COMPLETED)
                && order.getPaymentStatus() != PaymentStatus.PAID)
                throw new AdminOperationException("Phải xác nhận tiền chuyển khoản trước khi giao hàng");
            if (targetStatus == OrderStatus.CANCELLED && order.getPaymentStatus() != PaymentStatus.UNPAID)
                throw new AdminOperationException("Đơn đã báo chuyển khoản/đã thanh toán cần đối soát hoặc hoàn tiền thủ công trước; không hủy trực tiếp.");
        }
        if (targetStatus == OrderStatus.CANCELLED) {
            inventoryService.restoreCancelledOrder(order, performedBy);
            if (voucherService != null) voucherService.releaseForCancelledOrder(order.getId());
        }
        if (targetStatus == OrderStatus.COMPLETED && order.getPaymentMethod() == PaymentMethod.COD) {
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaidAt(LocalDateTime.now());
        }
        order.setStatus(targetStatus);
        eventPublisher.publishEvent(OrderStatusChangedMailEvent.from(order));
        return order;
    }

    @Transactional(readOnly = true)
    public Page<User> findUsers(String keyword, int page) {
        PageRequest pageable = PageRequest.of(normalizePage(page), PAGE_SIZE,
            Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return userRepository.searchForAdmin(normalizeKeyword(keyword), pageable);
    }

    @Transactional
    public boolean toggleUserStatus(Long id, String currentAdminEmail) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản có id: " + id));
        if (user.getRole() == Role.ADMIN || user.getEmail().equalsIgnoreCase(currentAdminEmail)) {
            throw new AdminOperationException("Không được khóa hoặc mở khóa tài khoản Admin");
        }
        user.setStatus(!user.isStatus());
        return user.isStatus();
    }

    private Product applyProduct(Product product, AdminProductRequest request, String actor, boolean creating) {
        Category category = findCategory(request.getCategoryId());
        String sku = request.getSku().trim().toUpperCase(Locale.ROOT);
        boolean duplicatedSku = creating
            ? productRepository.existsBySkuIgnoreCase(sku)
            : productRepository.existsBySkuIgnoreCaseAndIdNot(sku, product.getId());
        if (duplicatedSku) {
            throw new AdminOperationException("SKU đã tồn tại");
        }
        product.setSku(sku);
        product.setName(request.getName().trim());
        product.setDescription(ProductDescriptionSanitizer.sanitize(request.getDescription()));
        product.setPrice(request.getPrice());
        if (creating) {
            product.setQuantity(0);
            product.setCreatedBy(normalizeActor(actor));
            product.setSupplier(null);
        }
        product.setLowStockThreshold(request.getLowStockThreshold());
        product.setImage(normalizeOptional(request.getImage()));
        product.setCategory(category);
        product.setBrand(request.getBrandId() == null ? null : brandRepository
            .findById(request.getBrandId())
            .orElseThrow(() -> new EntityNotFoundException(
                "Không tìm thấy thương hiệu có id: " + request.getBrandId())));
        product.setUnit(request.getUnit() == null ? ProductUnit.KILOGRAM : request.getUnit());
        product.setOrigin(normalizeOptional(request.getOrigin()));
        product.setUpdatedBy(normalizeActor(actor));
        product.replaceAdditionalImages(parseImageUrls(request.getAdditionalImageUrls()));
        product.setStatus(request.isStatus());
        return product;
    }

    private List<String> parseImageUrls(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return value.lines().map(String::trim).filter(line -> !line.isEmpty()).distinct().limit(8).toList();
    }

    private String normalizeActor(String actor) {
        return actor == null || actor.isBlank() ? "SYSTEM" : actor.trim();
    }

    private Category applyCategory(Category category, AdminCategoryRequest request) {
        category.setName(request.getName().trim());
        category.setDescription(normalizeOptional(request.getDescription()));
        category.setImage(normalizeOptional(request.getImage()));
        category.setStatus(request.isStatus());
        return category;
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy danh mục có id: " + id));
    }

    private void requireUniqueCategoryName(String name, Long currentId) {
        categoryRepository.findByNameIgnoreCase(name.trim()).ifPresent(existing -> {
            if (currentId == null || !existing.getId().equals(currentId)) {
                throw new AdminOperationException("Tên danh mục đã tồn tại");
            }
        });
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String customerLabel(Order order) {
        if (order.getUser() == null) {
            return fallback(order.getReceiverName(), "Khách hàng không xác định");
        }
        String name = fallback(order.getUser().getFullName(), "Khách hàng");
        return order.getUser().getEmail() == null ? name : name + " (" + order.getUser().getEmail() + ")";
    }

    private void mergeSales(Map<String, MutableSales> values, String label, long units, BigDecimal revenue) {
        values.computeIfAbsent(label, ignored -> new MutableSales()).add(units, revenue);
    }

    private void mergeRevenue(Map<String, BigDecimal> values, String label, BigDecimal revenue) {
        values.merge(label, revenue, BigDecimal::add);
    }

    private List<AdminReportView.SalesRow> salesRows(Map<String, MutableSales> values) {
        return values.entrySet().stream()
            .map(entry -> new AdminReportView.SalesRow(
                entry.getKey(), entry.getValue().units, entry.getValue().revenue))
            .sorted(Comparator.comparing(AdminReportView.SalesRow::revenue).reversed()
                .thenComparing(AdminReportView.SalesRow::label))
            .toList();
    }

    private List<AdminReportView.RevenuePoint> revenuePoints(Map<String, BigDecimal> values) {
        return values.entrySet().stream()
            .map(entry -> new AdminReportView.RevenuePoint(entry.getKey(), entry.getValue()))
            .toList();
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void appendSalesSection(
        StringBuilder csv,
        String title,
        List<AdminReportView.SalesRow> rows
    ) {
        csv.append('\n');
        appendCsvRow(csv, title, "Số lượng bán", "Doanh thu");
        for (AdminReportView.SalesRow row : rows) {
            appendCsvRow(csv, row.label(), Long.toString(row.units()), row.revenue().toPlainString());
        }
    }

    private void appendCsvRow(StringBuilder csv, String... cells) {
        for (int index = 0; index < cells.length; index++) {
            if (index > 0) {
                csv.append(',');
            }
            String cell = cells[index] == null ? "" : cells[index];
            csv.append('"').append(cell.replace("\"", "\"\"")).append('"');
        }
        csv.append("\r\n");
    }

    private static final class MutableSales {
        private long units;
        private BigDecimal revenue = BigDecimal.ZERO;

        private void add(long addedUnits, BigDecimal addedRevenue) {
            units += addedUnits;
            revenue = revenue.add(addedRevenue);
        }
    }
}
