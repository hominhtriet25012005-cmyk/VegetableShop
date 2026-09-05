package com.vegetableshop.service;

import com.vegetableshop.dto.InventoryMovementRequest;
import com.vegetableshop.dto.InventoryOverviewView;
import com.vegetableshop.entity.Order;
import com.vegetableshop.entity.OrderDetail;
import com.vegetableshop.entity.InventoryDocument;
import com.vegetableshop.entity.InventoryDocumentItem;
import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.StockMovement;
import com.vegetableshop.entity.StockMovementType;
import com.vegetableshop.exception.AdminOperationException;
import com.vegetableshop.exception.ProductNotFoundException;
import com.vegetableshop.repository.ProductRepository;
import com.vegetableshop.repository.StockMovementRepository;
import com.vegetableshop.repository.StockMovementSpecifications;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@Profile("mysql")
public class InventoryService {

    private static final int HISTORY_PAGE_SIZE = 20;
    private static final int OVERVIEW_PAGE_SIZE = 10;

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

    public InventoryService(
        ProductRepository productRepository,
        StockMovementRepository stockMovementRepository
    ) {
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    @Transactional(readOnly = true)
    public InventoryOverviewView overview(String keyword, String requestedStockStatus) {
        return overview(keyword, requestedStockStatus, 0);
    }

    @Transactional(readOnly = true)
    public InventoryOverviewView overview(String keyword, String requestedStockStatus, int requestedPage) {
        List<InventoryOverviewView.ProductRow> allRows = productRepository.findAllByOrderByQuantityAscNameAsc()
            .stream()
            .map(this::toRow)
            .toList();

        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String stockStatus = normalizeStockStatus(requestedStockStatus);
        List<InventoryOverviewView.ProductRow> visibleRows = allRows.stream()
            .filter(row -> normalizedKeyword.isEmpty()
                || row.productName().toLowerCase(Locale.ROOT).contains(normalizedKeyword))
            .filter(row -> switch (stockStatus) {
                case "LOW" -> row.lowStock();
                case "OUT" -> row.outOfStock();
                case "HEALTHY" -> row.healthyStock();
                default -> true;
            })
            .toList();

        int totalPages = Math.max(1, (visibleRows.size() + OVERVIEW_PAGE_SIZE - 1) / OVERVIEW_PAGE_SIZE);
        int pageNumber = Math.min(Math.max(requestedPage, 0), totalPages - 1);
        int fromIndex = Math.min(pageNumber * OVERVIEW_PAGE_SIZE, visibleRows.size());
        int toIndex = Math.min(fromIndex + OVERVIEW_PAGE_SIZE, visibleRows.size());

        return new InventoryOverviewView(
            allRows.stream().mapToLong(InventoryOverviewView.ProductRow::quantity).sum(),
            allRows.stream().filter(InventoryOverviewView.ProductRow::lowStock).count(),
            allRows.stream().filter(InventoryOverviewView.ProductRow::outOfStock).count(),
            visibleRows.subList(fromIndex, toIndex),
            pageNumber,
            totalPages
        );
    }

    @Transactional(readOnly = true)
    public InventoryOverviewView.ProductRow findProductRow(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
        product.getCategory().getName();
        if (product.getBrand() != null) {
            product.getBrand().getName();
        }
        return toRow(product);
    }

    @Transactional(readOnly = true)
    public List<Product> productsForFilter() {
        return productRepository.findAll(Sort.by(Sort.Direction.ASC, "name", "id"));
    }

    @Transactional(readOnly = true)
    public Page<StockMovement> history(
        Long productId,
        StockMovementType movementType,
        LocalDate fromDate,
        LocalDate toDate,
        int page
    ) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new AdminOperationException("Ngày bắt đầu không được sau ngày kết thúc");
        }
        return stockMovementRepository.findAll(
            StockMovementSpecifications.withFilters(productId, movementType, fromDate, toDate),
            PageRequest.of(Math.max(page, 0), HISTORY_PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt", "id"))
        );
    }

    @Transactional
    public StockMovement applyManualMovement(
        Long productId,
        InventoryMovementRequest request,
        String performedBy
    ) {
        if (request.getMovementType() == null || !request.getMovementType().isManual()) {
            throw new AdminOperationException("Loại nghiệp vụ kho không hợp lệ");
        }
        Product product = lockProduct(productId);
        int before = safeQuantity(product);
        int requestedQuantity = request.getQuantity() == null ? -1 : request.getQuantity();
        int after;

        switch (request.getMovementType()) {
            case INBOUND -> {
                requirePositive(requestedQuantity);
                after = Math.addExact(before, requestedQuantity);
            }
            case OUTBOUND -> {
                requirePositive(requestedQuantity);
                if (requestedQuantity > before) {
                    throw new AdminOperationException(
                        "Không thể xuất " + requestedQuantity + " vì chỉ còn " + before + " sản phẩm"
                    );
                }
                after = before - requestedQuantity;
            }
            case ADJUSTMENT -> after = requestedQuantity;
            default -> throw new AdminOperationException("Loại nghiệp vụ kho không hợp lệ");
        }

        if (after < 0) {
            throw new AdminOperationException("Tồn kho sau điều chỉnh không được âm");
        }
        if (after == before) {
            throw new AdminOperationException("Số lượng mới không làm thay đổi tồn kho");
        }

        product.setQuantity(after);
        return saveMovement(product, request.getMovementType(), before, after,
            request.getReason(), null, null, performedBy);
    }

    @Transactional
    public void updateLowStockThreshold(Long productId, int threshold) {
        if (threshold < 0 || threshold > 1_000_000) {
            throw new AdminOperationException("Ngưỡng cảnh báo tồn kho không hợp lệ");
        }
        lockProduct(productId).setLowStockThreshold(threshold);
    }

    public StockMovement recordInitial(Product product, String performedBy) {
        int after = safeQuantity(product);
        return saveMovement(product, StockMovementType.INITIAL, 0, after,
            "Khởi tạo tồn kho khi tạo sản phẩm", "PRODUCT", String.valueOf(product.getId()), performedBy);
    }

    public StockMovement recordAdministrativeAdjustment(
        Product product,
        int quantityBefore,
        String performedBy
    ) {
        int after = safeQuantity(product);
        if (after == quantityBefore) {
            return null;
        }
        return saveMovement(product, StockMovementType.ADJUSTMENT, quantityBefore, after,
            "Điều chỉnh từ biểu mẫu sản phẩm", "PRODUCT", String.valueOf(product.getId()), performedBy);
    }

    public StockMovement recordSale(
        Product product,
        int quantityBefore,
        int soldQuantity,
        String orderCode,
        String customerEmail
    ) {
        if (soldQuantity <= 0 || safeQuantity(product) != quantityBefore - soldQuantity) {
            throw new AdminOperationException("Dữ liệu xuất kho của đơn hàng không nhất quán");
        }
        return saveMovement(product, StockMovementType.SALE, quantityBefore, safeQuantity(product),
            "Xuất kho cho đơn hàng " + orderCode, "ORDER", orderCode, customerEmail);
    }

    public StockMovement recordDocumentMovement(
        Product product,
        StockMovementType movementType,
        int quantityBefore,
        int quantityAfter,
        InventoryDocument document,
        InventoryDocumentItem item,
        String performedBy
    ) {
        StockMovement movement = saveMovement(product, movementType, quantityBefore, quantityAfter,
            document.getNote(), "INVENTORY_DOCUMENT", document.getCode(), performedBy);
        movement.setInventoryDocument(document);
        movement.setInventoryDocumentItem(item);
        return movement;
    }

    public void restoreCancelledOrder(Order order, String performedBy) {
        List<OrderDetail> details = order.getDetails().stream()
            .sorted(Comparator.comparing(detail -> detail.getProduct().getId()))
            .toList();
        for (OrderDetail detail : details) {
            Product product = lockProduct(detail.getProduct().getId());
            int before = safeQuantity(product);
            int after = Math.addExact(before, detail.getQuantity());
            product.setQuantity(after);
            saveMovement(product, StockMovementType.RETURN, before, after,
                "Hoàn kho do hủy đơn hàng " + order.getOrderCode(),
                "ORDER", order.getOrderCode(), performedBy);
        }
    }

    private StockMovement saveMovement(
        Product product,
        StockMovementType movementType,
        int before,
        int after,
        String reason,
        String referenceType,
        String referenceId,
        String performedBy
    ) {
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setProductName(product.getName());
        movement.setMovementType(movementType);
        movement.setQuantityBefore(before);
        movement.setQuantityChange(after - before);
        movement.setQuantityAfter(after);
        movement.setReason(normalizeRequired(reason, "Không có lý do"));
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setPerformedBy(normalizeRequired(performedBy, "SYSTEM"));
        return stockMovementRepository.save(movement);
    }

    private Product lockProduct(Long productId) {
        return productRepository.findByIdForUpdate(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private InventoryOverviewView.ProductRow toRow(Product product) {
        return new InventoryOverviewView.ProductRow(
            product.getId(),
            product.getName(),
            product.getCategory() == null ? "Chưa phân loại" : product.getCategory().getName(),
            product.getBrand() == null ? "Không thương hiệu" : product.getBrand().getName(),
            safeQuantity(product),
            product.getLowStockThreshold() == null ? 10 : product.getLowStockThreshold(),
            product.isStatus()
        );
    }

    private int safeQuantity(Product product) {
        return product.getQuantity() == null ? 0 : product.getQuantity();
    }

    private void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new AdminOperationException("Số lượng nhập hoặc xuất phải lớn hơn 0");
        }
    }

    private String normalizeStockStatus(String stockStatus) {
        if (stockStatus == null) {
            return "ALL";
        }
        return switch (stockStatus.trim().toUpperCase(Locale.ROOT)) {
            case "LOW", "OUT", "HEALTHY" -> stockStatus.trim().toUpperCase(Locale.ROOT);
            default -> "ALL";
        };
    }

    private String normalizeRequired(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
