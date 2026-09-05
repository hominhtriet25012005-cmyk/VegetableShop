package com.vegetableshop.service;

import com.vegetableshop.dto.AdminReportView;
import com.vegetableshop.dto.InventoryDocumentItemRequest;
import com.vegetableshop.dto.InventoryDocumentRequest;
import com.vegetableshop.entity.InventoryDocument;
import com.vegetableshop.entity.InventoryDocumentItem;
import com.vegetableshop.entity.InventoryDocumentStatus;
import com.vegetableshop.entity.InventoryDocumentType;
import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.StockMovementType;
import com.vegetableshop.entity.Supplier;
import com.vegetableshop.exception.AdminOperationException;
import com.vegetableshop.exception.ProductNotFoundException;
import com.vegetableshop.repository.InventoryDocumentRepository;
import com.vegetableshop.repository.InventoryDocumentSpecifications;
import com.vegetableshop.repository.ProductRepository;
import com.vegetableshop.repository.SupplierRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Profile("mysql")
public class InventoryDocumentService {

    private static final int PAGE_SIZE = 15;
    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final InventoryDocumentRepository documentRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryService inventoryService;

    public InventoryDocumentService(
        InventoryDocumentRepository documentRepository,
        ProductRepository productRepository,
        SupplierRepository supplierRepository,
        InventoryService inventoryService
    ) {
        this.documentRepository = documentRepository;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public Page<InventoryDocument> documents(
        String keyword,
        InventoryDocumentType type,
        InventoryDocumentStatus status,
        Long supplierId,
        Long productId,
        LocalDate from,
        LocalDate to,
        int page
    ) {
        validateDateRange(from, to);
        Page<InventoryDocument> result = documentRepository.findAll(
            InventoryDocumentSpecifications.withFilters(keyword, type, status, supplierId, productId, from, to),
            PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt", "id"))
        );
        result.getContent().forEach(document -> document.getItems().size());
        return result;
    }

    /**
     * Compatibility overload for callers that do not filter documents by product.
     */
    @Transactional(readOnly = true)
    public Page<InventoryDocument> documents(
        String keyword,
        InventoryDocumentType type,
        InventoryDocumentStatus status,
        Long supplierId,
        LocalDate from,
        LocalDate to,
        int page
    ) {
        return documents(keyword, type, status, supplierId, null, from, to, page);
    }

    @Transactional(readOnly = true)
    public Product productForFilter(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    @Transactional(readOnly = true)
    public InventoryDocument findDetailed(Long id) {
        return documentRepository.findDetailedById(id)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phiếu kho có id: " + id));
    }

    @Transactional(readOnly = true)
    public InventoryDocumentRequest form(Long id) {
        InventoryDocument document = findDetailed(id);
        ensureDraft(document);
        InventoryDocumentRequest request = new InventoryDocumentRequest();
        request.setType(document.getType());
        request.setSupplierId(document.getSupplier() == null ? null : document.getSupplier().getId());
        request.setInvoiceReference(document.getInvoiceReference());
        request.setNote(document.getNote());
        List<InventoryDocumentItemRequest> items = new ArrayList<>();
        for (InventoryDocumentItem item : document.getItems()) {
            InventoryDocumentItemRequest line = new InventoryDocumentItemRequest();
            line.setProductId(item.getProduct().getId());
            line.setQuantity(item.getQuantity());
            line.setUnitCost(item.getUnitCost());
            items.add(line);
        }
        request.setItems(items);
        return request;
    }

    @Transactional(readOnly = true)
    public List<Product> productsForForm() {
        return productRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Supplier> suppliersForForm() {
        return supplierRepository.findAllByOrderByNameAsc();
    }

    @Transactional
    public InventoryDocument createDraft(InventoryDocumentRequest request, String actor) {
        validateRequest(request);
        InventoryDocument document = new InventoryDocument();
        document.setCode("TMP-" + UUID.randomUUID().toString().replace("-", ""));
        document.setStatus(InventoryDocumentStatus.DRAFT);
        document.setCreatedBy(normalizeActor(actor));
        mapDraft(document, request);
        InventoryDocument saved = documentRepository.saveAndFlush(document);
        saved.setCode(generateCode(saved));
        return saved;
    }

    @Transactional
    public InventoryDocument updateDraft(Long id, InventoryDocumentRequest request) {
        validateRequest(request);
        InventoryDocument document = documentRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phiếu kho có id: " + id));
        ensureDraft(document);
        document.clearItems();
        documentRepository.flush();
        mapDraft(document, request);
        document.setCode(generateCode(document));
        return document;
    }

    @Transactional
    public void deleteDraft(Long id) {
        InventoryDocument document = documentRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phiếu kho có id: " + id));
        ensureDraft(document);
        documentRepository.delete(document);
    }

    @Transactional
    public InventoryDocument post(Long id, String actor) {
        InventoryDocument document = documentRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phiếu kho có id: " + id));
        ensureDraft(document);
        if (document.getItems().isEmpty()) {
            throw new AdminOperationException("Phiếu kho chưa có sản phẩm");
        }

        List<InventoryDocumentItem> orderedItems = document.getItems().stream()
            .sorted((left, right) -> left.getProduct().getId().compareTo(right.getProduct().getId()))
            .toList();
        for (InventoryDocumentItem item : orderedItems) {
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                .orElseThrow(() -> new ProductNotFoundException(item.getProduct().getId()));
            int before = product.getQuantity() == null ? 0 : product.getQuantity();
            int requested = item.getQuantity();
            int after;
            StockMovementType movementType;
            switch (document.getType()) {
                case INBOUND -> {
                    after = Math.addExact(before, requested);
                    movementType = StockMovementType.INBOUND;
                }
                case OUTBOUND -> {
                    if (requested > before) {
                        throw new AdminOperationException("Không thể xuất " + requested + " " + product.getName()
                            + " vì chỉ còn " + before);
                    }
                    after = before - requested;
                    movementType = StockMovementType.OUTBOUND;
                }
                case ADJUSTMENT -> {
                    after = requested;
                    movementType = StockMovementType.ADJUSTMENT;
                    if (after == before) {
                        throw new AdminOperationException("Số kiểm kê của " + product.getName()
                            + " không làm thay đổi tồn kho");
                    }
                }
                default -> throw new AdminOperationException("Loại phiếu kho không hợp lệ");
            }
            if (after < 0) {
                throw new AdminOperationException("Tồn kho sau ghi nhận không được âm");
            }
            product.setQuantity(after);
            item.setProduct(product);
            item.setQuantityBefore(before);
            item.setQuantityChange(after - before);
            item.setQuantityAfter(after);
            inventoryService.recordDocumentMovement(product, movementType, before, after,
                document, item, normalizeActor(actor));
        }

        document.setStatus(InventoryDocumentStatus.POSTED);
        document.setPostedBy(normalizeActor(actor));
        document.setPostedAt(LocalDateTime.now());
        return document;
    }

    @Transactional(readOnly = true)
    public List<AdminReportView.SalesRow> supplierPurchases(LocalDate from, LocalDate to) {
        LocalDate effectiveFrom = from == null ? LocalDate.now().withDayOfYear(1) : from;
        LocalDate effectiveTo = to == null ? LocalDate.now() : to;
        List<InventoryDocument> documents = documentRepository
            .findByTypeAndStatusAndPostedAtGreaterThanEqualAndPostedAtLessThan(
                InventoryDocumentType.INBOUND, InventoryDocumentStatus.POSTED,
                effectiveFrom.atStartOfDay(), effectiveTo.plusDays(1).atStartOfDay());
        Map<String, PurchaseSummary> summaries = new LinkedHashMap<>();
        for (InventoryDocument document : documents) {
            String supplier = document.getSupplier() == null ? "Không xác định" : document.getSupplier().getName();
            PurchaseSummary summary = summaries.computeIfAbsent(supplier, ignored -> new PurchaseSummary());
            summary.units += document.getTotalQuantity();
            summary.value = summary.value.add(document.getTotalCost());
        }
        return summaries.entrySet().stream()
            .map(entry -> new AdminReportView.SalesRow(entry.getKey(), entry.getValue().units, entry.getValue().value))
            .toList();
    }

    private void mapDraft(InventoryDocument document, InventoryDocumentRequest request) {
        document.setType(request.getType());
        document.setSupplier(resolveSupplier(request));
        document.setInvoiceReference(normalizeNullable(request.getInvoiceReference()));
        document.setNote(normalizeNullable(request.getNote()));
        document.clearItems();
        for (InventoryDocumentItemRequest line : request.getItems()) {
            Product product = productRepository.findById(line.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(line.getProductId()));
            InventoryDocumentItem item = new InventoryDocumentItem();
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setProductSku(product.getSku());
            item.setQuantity(line.getQuantity());
            item.setUnitCost(request.getType() == InventoryDocumentType.INBOUND ? line.getUnitCost() : null);
            document.addItem(item);
        }
    }

    private Supplier resolveSupplier(InventoryDocumentRequest request) {
        if (request.getType() == InventoryDocumentType.INBOUND) {
            if (request.getSupplierId() == null) {
                throw new AdminOperationException("Phiếu nhập kho phải chọn nhà cung cấp");
            }
            Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new EntityNotFoundException(
                    "Không tìm thấy nhà cung cấp có id: " + request.getSupplierId()));
            if (!supplier.isStatus()) {
                throw new AdminOperationException("Nhà cung cấp đã ngừng hợp tác");
            }
            return supplier;
        }
        return null;
    }

    private void validateRequest(InventoryDocumentRequest request) {
        if (request.getType() == null) {
            throw new AdminOperationException("Vui lòng chọn loại phiếu");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new AdminOperationException("Phiếu kho phải có ít nhất một sản phẩm");
        }
        Set<Long> productIds = new HashSet<>();
        for (InventoryDocumentItemRequest item : request.getItems()) {
            if (item.getProductId() == null) {
                throw new AdminOperationException("Vui lòng chọn đầy đủ sản phẩm");
            }
            if (!productIds.add(item.getProductId())) {
                throw new AdminOperationException("Một sản phẩm không được xuất hiện hai lần trong cùng phiếu");
            }
            if (item.getQuantity() == null || item.getQuantity() < 0 || item.getQuantity() > 1_000_000) {
                throw new AdminOperationException("Số lượng sản phẩm không hợp lệ");
            }
            if (request.getType() != InventoryDocumentType.ADJUSTMENT && item.getQuantity() == 0) {
                throw new AdminOperationException("Số lượng nhập hoặc xuất phải lớn hơn 0");
            }
            if (request.getType() == InventoryDocumentType.INBOUND
                && (item.getUnitCost() == null || item.getUnitCost().compareTo(BigDecimal.ZERO) < 0)) {
                throw new AdminOperationException("Phiếu nhập phải có giá vốn hợp lệ cho từng sản phẩm");
            }
        }
    }

    private String generateCode(InventoryDocument document) {
        LocalDate date = document.getCreatedAt() == null ? LocalDate.now() : document.getCreatedAt().toLocalDate();
        return document.getType().getCodePrefix() + "-" + CODE_DATE.format(date)
            + "-" + String.format(Locale.ROOT, "%06d", document.getId());
    }

    private void ensureDraft(InventoryDocument document) {
        if (document.getStatus() != InventoryDocumentStatus.DRAFT) {
            throw new AdminOperationException("Phiếu đã xác nhận nên không thể sửa hoặc xóa");
        }
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new AdminOperationException("Ngày bắt đầu không được sau ngày kết thúc");
        }
    }

    private String normalizeActor(String actor) {
        return actor == null || actor.isBlank() ? "SYSTEM" : actor.trim();
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static final class PurchaseSummary {
        private long units;
        private BigDecimal value = BigDecimal.ZERO;
    }
}
