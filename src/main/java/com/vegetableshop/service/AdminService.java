package com.vegetableshop.service;

import com.vegetableshop.dto.AdminCategoryRequest;
import com.vegetableshop.dto.AdminDashboardView;
import com.vegetableshop.dto.AdminProductRequest;
import com.vegetableshop.entity.Category;
import com.vegetableshop.entity.Order;
import com.vegetableshop.entity.OrderDetail;
import com.vegetableshop.entity.OrderStatus;
import com.vegetableshop.entity.PaymentMethod;
import com.vegetableshop.entity.PaymentStatus;
import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.Role;
import com.vegetableshop.entity.User;
import com.vegetableshop.entity.Supplier;
import com.vegetableshop.exception.AdminOperationException;
import com.vegetableshop.exception.OrderNotFoundException;
import com.vegetableshop.exception.ProductNotFoundException;
import com.vegetableshop.repository.CategoryRepository;
import com.vegetableshop.repository.OrderRepository;
import com.vegetableshop.repository.AdminProductSpecifications;
import com.vegetableshop.repository.ProductRepository;
import com.vegetableshop.repository.UserRepository;
import com.vegetableshop.repository.SupplierRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@Profile("mysql")
public class AdminService {

    private static final int PAGE_SIZE = 10;
    private static final int LOW_STOCK_LIMIT = 10;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final SupplierRepository supplierRepository;

    public AdminService(
        ProductRepository productRepository,
        CategoryRepository categoryRepository,
        OrderRepository orderRepository,
        UserRepository userRepository,
        SupplierRepository supplierRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.supplierRepository = supplierRepository;
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
            productRepository.findTop5ByStatusTrueAndQuantityLessThanEqualOrderByQuantityAsc(LOW_STOCK_LIMIT)
        );
    }

    @Transactional(readOnly = true)
    public Page<Product> findProducts(String keyword, Long categoryId, Boolean status, int page) {
        String normalized = normalizeKeyword(keyword);
        Long normalizedCategoryId = categoryId != null && categoryId > 0 ? categoryId : null;
        PageRequest pageable = PageRequest.of(normalizePage(page), PAGE_SIZE,
            Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return productRepository.findAll(
            AdminProductSpecifications.withFilters(normalized, normalizedCategoryId, status),
            pageable
        );
    }

    @Transactional(readOnly = true)
    public AdminProductRequest getProductForm(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        product.getCategory().getName();
        return AdminProductRequest.from(product);
    }

    @Transactional
    public Product createProduct(AdminProductRequest request) {
        return productRepository.save(applyProduct(new Product(), request));
    }

    @Transactional
    public Product updateProduct(Long id, AdminProductRequest request) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        return productRepository.save(applyProduct(product, request));
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
        Order order = orderRepository.findAdminById(id)
            .orElseThrow(() -> new OrderNotFoundException(id));
        if (targetStatus == null || !allowedTransitions(order.getStatus()).contains(targetStatus)) {
            throw new AdminOperationException(
                "Không thể chuyển đơn từ " + order.getStatus() + " sang " + targetStatus
            );
        }

        if (targetStatus == OrderStatus.CANCELLED) {
            restoreStock(order);
        }
        if (targetStatus == OrderStatus.COMPLETED && order.getPaymentMethod() == PaymentMethod.COD) {
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaidAt(LocalDateTime.now());
        }
        order.setStatus(targetStatus);
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

    private Product applyProduct(Product product, AdminProductRequest request) {
        Category category = findCategory(request.getCategoryId());
        product.setName(request.getName().trim());
        product.setDescription(normalizeOptional(request.getDescription()));
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        product.setImage(normalizeOptional(request.getImage()));
        product.setCategory(category);
        product.setSupplier(request.getSupplierId() == null ? null : supplierRepository
            .findById(request.getSupplierId())
            .orElseThrow(() -> new EntityNotFoundException(
                "Không tìm thấy nhà cung cấp có id: " + request.getSupplierId())));
        product.setStatus(request.isStatus());
        return product;
    }

    private Category applyCategory(Category category, AdminCategoryRequest request) {
        category.setName(request.getName().trim());
        category.setDescription(normalizeOptional(request.getDescription()));
        category.setImage(normalizeOptional(request.getImage()));
        category.setStatus(request.isStatus());
        return category;
    }

    private void restoreStock(Order order) {
        List<OrderDetail> details = order.getDetails().stream()
            .sorted(Comparator.comparing(detail -> detail.getProduct().getId()))
            .toList();
        for (OrderDetail detail : details) {
            Product product = productRepository.findByIdForUpdate(detail.getProduct().getId())
                .orElseThrow(() -> new AdminOperationException(
                    "Không thể hoàn tồn kho vì sản phẩm không còn tồn tại"
                ));
            product.setQuantity(product.getQuantity() + detail.getQuantity());
        }
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
}
