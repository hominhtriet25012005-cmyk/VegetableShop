package com.vegetableshop.service;

import com.vegetableshop.dto.InventoryDocumentItemRequest;
import com.vegetableshop.dto.InventoryDocumentRequest;
import com.vegetableshop.entity.InventoryDocument;
import com.vegetableshop.entity.InventoryDocumentItem;
import com.vegetableshop.entity.InventoryDocumentStatus;
import com.vegetableshop.entity.InventoryDocumentType;
import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.ProductUnit;
import com.vegetableshop.entity.StockMovementType;
import com.vegetableshop.entity.Supplier;
import com.vegetableshop.exception.AdminOperationException;
import com.vegetableshop.repository.InventoryDocumentRepository;
import com.vegetableshop.repository.ProductRepository;
import com.vegetableshop.repository.SupplierRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryDocumentServiceTests {

    @Mock private InventoryDocumentRepository documentRepository;
    @Mock private ProductRepository productRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private InventoryService inventoryService;

    @Test
    void savingInboundDraftDoesNotChangeStock() {
        Product product = product(1L, 10);
        Supplier supplier = supplier();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(supplierRepository.findById(2L)).thenReturn(Optional.of(supplier));
        when(documentRepository.saveAndFlush(any(InventoryDocument.class))).thenAnswer(invocation -> {
            InventoryDocument document = invocation.getArgument(0);
            document.setId(8L);
            ReflectionTestUtils.setField(document, "createdAt", LocalDateTime.of(2026, 8, 30, 14, 0));
            return document;
        });

        InventoryDocument document = service().createDraft(request(InventoryDocumentType.INBOUND, 5),
            " admin@example.com ");

        assertEquals(10, product.getQuantity());
        assertEquals("PN-20260830-000008", document.getCode());
        assertEquals(InventoryDocumentStatus.DRAFT, document.getStatus());
        assertEquals("admin@example.com", document.getCreatedBy());
        assertEquals(1, document.getItems().size());
        verify(inventoryService, never()).recordDocumentMovement(any(), any(), any(Integer.class),
            any(Integer.class), any(), any(), any());
    }

    @Test
    void postingInboundDocumentUpdatesStockAndCreatesLinkedLedgerEntry() {
        Product product = product(1L, 10);
        InventoryDocument document = draft(InventoryDocumentType.INBOUND, product, 5);
        when(documentRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(document));
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        InventoryDocument posted = service().post(7L, "admin@example.com");

        InventoryDocumentItem item = posted.getItems().getFirst();
        assertEquals(15, product.getQuantity());
        assertEquals(InventoryDocumentStatus.POSTED, posted.getStatus());
        assertEquals("admin@example.com", posted.getPostedBy());
        assertNotNull(posted.getPostedAt());
        assertEquals(10, item.getQuantityBefore());
        assertEquals(5, item.getQuantityChange());
        assertEquals(15, item.getQuantityAfter());
        verify(inventoryService).recordDocumentMovement(product, StockMovementType.INBOUND,
            10, 15, document, item, "admin@example.com");
    }

    @Test
    void outboundDocumentCannotMakeStockNegative() {
        Product product = product(1L, 3);
        InventoryDocument document = draft(InventoryDocumentType.OUTBOUND, product, 4);
        when(documentRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(document));
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        AdminOperationException exception = assertThrows(AdminOperationException.class,
            () -> service().post(7L, "admin@example.com"));

        assertEquals(3, product.getQuantity());
        assertEquals(InventoryDocumentStatus.DRAFT, document.getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("chỉ còn 3"));
        verify(inventoryService, never()).recordDocumentMovement(any(), any(), any(Integer.class),
            any(Integer.class), any(), any(), any());
    }

    @Test
    void postedDocumentCannotBePostedTwice() {
        InventoryDocument document = draft(InventoryDocumentType.INBOUND, product(1L, 10), 5);
        document.setStatus(InventoryDocumentStatus.POSTED);
        when(documentRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(document));

        assertThrows(AdminOperationException.class, () -> service().post(7L, "admin@example.com"));

        verify(productRepository, never()).findByIdForUpdate(any());
        verify(inventoryService, never()).recordDocumentMovement(any(), any(), any(Integer.class),
            any(Integer.class), any(), any(), any());
    }

    @Test
    void inboundDraftRequiresActiveSupplier() {
        InventoryDocumentRequest request = request(InventoryDocumentType.INBOUND, 5);
        request.setSupplierId(null);

        assertThrows(AdminOperationException.class,
            () -> service().createDraft(request, "admin@example.com"));

        verify(documentRepository, never()).saveAndFlush(any());
    }

    private InventoryDocumentService service() {
        return new InventoryDocumentService(documentRepository, productRepository, supplierRepository,
            inventoryService);
    }

    private InventoryDocumentRequest request(InventoryDocumentType type, int quantity) {
        InventoryDocumentItemRequest line = new InventoryDocumentItemRequest();
        line.setProductId(1L);
        line.setQuantity(quantity);
        line.setUnitCost(new BigDecimal("20000"));
        InventoryDocumentRequest request = new InventoryDocumentRequest();
        request.setType(type);
        request.setSupplierId(type == InventoryDocumentType.INBOUND ? 2L : null);
        request.setNote("Kiểm thử phiếu kho");
        request.setItems(List.of(line));
        return request;
    }

    private InventoryDocument draft(InventoryDocumentType type, Product product, int quantity) {
        InventoryDocument document = new InventoryDocument();
        document.setId(7L);
        document.setCode(type.getCodePrefix() + "-20260830-000007");
        document.setType(type);
        document.setStatus(InventoryDocumentStatus.DRAFT);
        document.setCreatedBy("admin@example.com");
        InventoryDocumentItem item = new InventoryDocumentItem();
        item.setProduct(product);
        item.setProductName(product.getName());
        item.setProductSku(product.getSku());
        item.setQuantity(quantity);
        item.setUnitCost(type == InventoryDocumentType.INBOUND ? new BigDecimal("20000") : null);
        document.addItem(item);
        return document;
    }

    private Product product(Long id, int quantity) {
        Product product = new Product();
        product.setId(id);
        product.setSku("SP-" + id);
        product.setName("Sản phẩm " + id);
        product.setQuantity(quantity);
        product.setUnit(ProductUnit.KILOGRAM);
        return product;
    }

    private Supplier supplier() {
        Supplier supplier = new Supplier();
        supplier.setId(2L);
        supplier.setCode("NCC-002");
        supplier.setName("Nông trại kiểm thử");
        supplier.setStatus(true);
        return supplier;
    }
}
