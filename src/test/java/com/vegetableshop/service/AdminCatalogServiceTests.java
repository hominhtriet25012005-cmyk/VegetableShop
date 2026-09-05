package com.vegetableshop.service;

import com.vegetableshop.dto.AdminBrandRequest;
import com.vegetableshop.dto.AdminSupplierRequest;
import com.vegetableshop.entity.Brand;
import com.vegetableshop.entity.Supplier;
import com.vegetableshop.exception.AdminOperationException;
import com.vegetableshop.repository.BrandRepository;
import com.vegetableshop.repository.SupplierRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCatalogServiceTests {

    @Mock private BrandRepository brandRepository;
    @Mock private SupplierRepository supplierRepository;

    @Test
    void createsNormalizedBrand() {
        AdminBrandRequest request = new AdminBrandRequest();
        request.setName("  Nông Sản Việt  ");
        request.setLogo("  https://example.test/logo.png  ");
        request.setStatus(true);
        when(brandRepository.findByNameIgnoreCase("Nông Sản Việt")).thenReturn(Optional.empty());
        when(brandRepository.save(any(Brand.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Brand brand = service().createBrand(request);

        assertEquals("Nông Sản Việt", brand.getName());
        assertEquals("https://example.test/logo.png", brand.getLogo());
    }

    @Test
    void rejectsDuplicateBrandNameIgnoringCase() {
        Brand existing = new Brand();
        existing.setId(1L);
        existing.setName("Nông Sản Việt");
        AdminBrandRequest request = new AdminBrandRequest();
        request.setName("nông sản việt");
        when(brandRepository.findByNameIgnoreCase("nông sản việt")).thenReturn(Optional.of(existing));

        assertThrows(AdminOperationException.class, () -> service().createBrand(request));

        verify(brandRepository, never()).save(any());
    }

    @Test
    void createsSupplierWithUppercaseCode() {
        AdminSupplierRequest request = new AdminSupplierRequest();
        request.setCode(" ncc-dl ");
        request.setName("  Nông trại Đà Lạt  ");
        request.setEmail("contact@example.test");
        when(supplierRepository.findByCodeIgnoreCase("NCC-DL")).thenReturn(Optional.empty());
        when(supplierRepository.findByNameIgnoreCase("Nông trại Đà Lạt")).thenReturn(Optional.empty());
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Supplier supplier = service().createSupplier(request);

        assertEquals("NCC-DL", supplier.getCode());
        assertEquals("Nông trại Đà Lạt", supplier.getName());
    }

    @Test
    void togglesSupplierWithoutDeletingHistory() {
        Supplier supplier = new Supplier();
        supplier.setId(2L);
        supplier.setStatus(true);
        when(supplierRepository.findById(2L)).thenReturn(Optional.of(supplier));

        assertFalse(service().toggleSupplier(2L));
        assertFalse(supplier.isStatus());
        verify(supplierRepository, never()).delete(any());
    }

    private AdminCatalogService service() {
        return new AdminCatalogService(brandRepository, supplierRepository);
    }
}
