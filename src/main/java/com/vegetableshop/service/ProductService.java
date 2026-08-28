package com.vegetableshop.service;

import com.vegetableshop.dto.ProductFilter;
import com.vegetableshop.entity.Category;
import com.vegetableshop.entity.Product;
import com.vegetableshop.exception.ProductNotFoundException;
import com.vegetableshop.repository.ProductRepository;
import com.vegetableshop.repository.ProductSpecifications;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Profile("mysql")
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> findAllActiveProducts() {
        return productRepository.findByStatusTrueOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Page<Product> search(ProductFilter filter) {
        filter.normalize();
        PageRequest pageRequest = PageRequest.of(
            filter.getPage(),
            filter.getSize(),
            resolveSort(filter.getSort())
        );
        return productRepository.findAll(ProductSpecifications.withFilters(filter), pageRequest);
    }

    @Transactional(readOnly = true)
    public Product findActiveProduct(Long id) {
        return productRepository.findByIdAndStatusTrueAndCategoryStatusTrue(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Product> findNewestProducts() {
        return productRepository.findTop3ByStatusTrueAndCategoryStatusTrueOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Product> findHomepageProducts() {
        return productRepository.findTop8ByStatusTrueAndCategoryStatusTrueOrderByCreatedAtDesc();
    }

    /**
     * Loads the default eight newest products plus up to eight products for
     * every active category. The result is de-duplicated while preserving its
     * order so the homepage can filter locally without another page load.
     */
    @Transactional(readOnly = true)
    public List<Product> findHomepageCatalog(List<Category> categories) {
        Map<Long, Product> productsById = new LinkedHashMap<>();
        findHomepageProducts().forEach(product -> productsById.put(product.getId(), product));

        categories.forEach(category -> productRepository
            .findTop8ByStatusTrueAndCategoryStatusTrueAndCategoryIdOrderByCreatedAtDesc(category.getId())
            .forEach(product -> productsById.putIfAbsent(product.getId(), product)));

        return List.copyOf(productsById.values());
    }

    @Transactional(readOnly = true)
    public List<Product> findRelatedProducts(Product product) {
        return productRepository
            .findTop4ByStatusTrueAndCategoryStatusTrueAndCategoryIdAndIdNotOrderByCreatedAtDesc(
                product.getCategory().getId(),
                product.getId()
            );
    }

    @Transactional(readOnly = true)
    public List<Product> findSameSupplierProducts(Product product) {
        if (product.getSupplier() == null || !product.getSupplier().isStatus()) {
            return List.of();
        }
        return productRepository
            .findTop4ByStatusTrueAndCategoryStatusTrueAndSupplierIdAndIdNotOrderByCreatedAtDesc(
                product.getSupplier().getId(), product.getId());
    }

    @Transactional(readOnly = true)
    public List<Product> findActiveProductsInOrder(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Product> products = productRepository
            .findByIdInAndStatusTrueAndCategoryStatusTrue(ids)
            .stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));
        return ids.stream().map(products::get).filter(product -> product != null).toList();
    }

    private Sort resolveSort(String sort) {
        return switch (sort) {
            case "priceAsc" -> Sort.by(Sort.Direction.ASC, "price", "id");
            case "priceDesc" -> Sort.by(Sort.Direction.DESC, "price", "id");
            case "nameAsc" -> Sort.by(Sort.Direction.ASC, "name", "id");
            default -> Sort.by(Sort.Direction.DESC, "createdAt", "id");
        };
    }
}
