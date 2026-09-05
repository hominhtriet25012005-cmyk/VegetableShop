package com.vegetableshop.service;

import com.vegetableshop.dto.AdminBrandRequest;
import com.vegetableshop.dto.AdminSupplierRequest;
import com.vegetableshop.entity.Brand;
import com.vegetableshop.entity.Supplier;
import com.vegetableshop.exception.AdminOperationException;
import com.vegetableshop.repository.BrandRepository;
import com.vegetableshop.repository.SupplierRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Profile("mysql")
public class AdminCatalogService {

    private final BrandRepository brandRepository;
    private final SupplierRepository supplierRepository;

    public AdminCatalogService(BrandRepository brandRepository, SupplierRepository supplierRepository) {
        this.brandRepository = brandRepository;
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public List<Brand> brands(String keyword) {
        String value = normalizeSearch(keyword);
        return brandRepository.findAllByOrderByNameAsc().stream()
            .filter(brand -> value.isEmpty() || brand.getName().toLowerCase(Locale.ROOT).contains(value))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<Brand> allBrands() {
        return brandRepository.findAllByOrderByNameAsc();
    }

    @Transactional
    public Brand createBrand(AdminBrandRequest request) {
        ensureBrandNameAvailable(request.getName(), null);
        Brand brand = new Brand();
        mapBrand(brand, request);
        return brandRepository.save(brand);
    }

    @Transactional(readOnly = true)
    public AdminBrandRequest brandForm(Long id) {
        return AdminBrandRequest.from(findBrand(id));
    }

    @Transactional
    public Brand updateBrand(Long id, AdminBrandRequest request) {
        ensureBrandNameAvailable(request.getName(), id);
        Brand brand = findBrand(id);
        mapBrand(brand, request);
        return brand;
    }

    @Transactional
    public boolean toggleBrand(Long id) {
        Brand brand = findBrand(id);
        brand.setStatus(!brand.isStatus());
        return brand.isStatus();
    }

    @Transactional(readOnly = true)
    public List<Supplier> suppliers(String keyword) {
        String value = normalizeSearch(keyword);
        return supplierRepository.findAllByOrderByNameAsc().stream()
            .filter(supplier -> value.isEmpty()
                || supplier.getName().toLowerCase(Locale.ROOT).contains(value)
                || supplier.getCode().toLowerCase(Locale.ROOT).contains(value))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<Supplier> allSuppliers() {
        return supplierRepository.findAllByOrderByNameAsc();
    }

    @Transactional
    public Supplier createSupplier(AdminSupplierRequest request) {
        ensureSupplierAvailable(request, null);
        Supplier supplier = new Supplier();
        mapSupplier(supplier, request);
        return supplierRepository.save(supplier);
    }

    @Transactional(readOnly = true)
    public AdminSupplierRequest supplierForm(Long id) {
        return AdminSupplierRequest.from(findSupplier(id));
    }

    @Transactional
    public Supplier updateSupplier(Long id, AdminSupplierRequest request) {
        ensureSupplierAvailable(request, id);
        Supplier supplier = findSupplier(id);
        mapSupplier(supplier, request);
        return supplier;
    }

    @Transactional
    public boolean toggleSupplier(Long id) {
        Supplier supplier = findSupplier(id);
        supplier.setStatus(!supplier.isStatus());
        return supplier.isStatus();
    }

    private Brand findBrand(Long id) {
        return brandRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy thương hiệu có id: " + id));
    }

    private Supplier findSupplier(Long id) {
        return supplierRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy nhà cung cấp có id: " + id));
    }

    private void ensureBrandNameAvailable(String requestedName, Long currentId) {
        String name = normalizeRequired(requestedName);
        brandRepository.findByNameIgnoreCase(name)
            .filter(existing -> currentId == null || !existing.getId().equals(currentId))
            .ifPresent(existing -> { throw new AdminOperationException("Tên thương hiệu đã tồn tại"); });
    }

    private void ensureSupplierAvailable(AdminSupplierRequest request, Long currentId) {
        String code = normalizeRequired(request.getCode()).toUpperCase(Locale.ROOT);
        String name = normalizeRequired(request.getName());
        supplierRepository.findByCodeIgnoreCase(code)
            .filter(existing -> currentId == null || !existing.getId().equals(currentId))
            .ifPresent(existing -> { throw new AdminOperationException("Mã nhà cung cấp đã tồn tại"); });
        supplierRepository.findByNameIgnoreCase(name)
            .filter(existing -> currentId == null || !existing.getId().equals(currentId))
            .ifPresent(existing -> { throw new AdminOperationException("Tên nhà cung cấp đã tồn tại"); });
    }

    private void mapBrand(Brand brand, AdminBrandRequest request) {
        brand.setName(normalizeRequired(request.getName()));
        brand.setLogo(normalizeNullable(request.getLogo()));
        brand.setDescription(normalizeNullable(request.getDescription()));
        brand.setStatus(request.isStatus());
    }

    private void mapSupplier(Supplier supplier, AdminSupplierRequest request) {
        supplier.setCode(normalizeRequired(request.getCode()).toUpperCase(Locale.ROOT));
        supplier.setName(normalizeRequired(request.getName()));
        supplier.setContactPerson(normalizeNullable(request.getContactPerson()));
        supplier.setPhone(normalizeNullable(request.getPhone()));
        supplier.setEmail(normalizeNullable(request.getEmail()));
        supplier.setAddress(normalizeNullable(request.getAddress()));
        supplier.setTaxCode(normalizeNullable(request.getTaxCode()));
        supplier.setStatus(request.isStatus());
    }

    private String normalizeSearch(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRequired(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
