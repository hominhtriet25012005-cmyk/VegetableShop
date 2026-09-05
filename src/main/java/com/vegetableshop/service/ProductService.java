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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Profile("mysql")
public class ProductService {

    private final ProductRepository productRepository;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private PromotionPricingService promotionPricingService;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> findAllActiveProducts() {
        return decorate(productRepository.findByStatusTrueOrderByCreatedAtDesc());
    }

    @Transactional(readOnly = true)
    public Page<Product> search(ProductFilter filter) {
        filter.normalize();
        PageRequest pageRequest = PageRequest.of(
            filter.getPage(),
            filter.getSize(),
            resolveSort(filter.getSort())
        );
        Page<Product> page=productRepository.findAll(ProductSpecifications.withFilters(filter), pageRequest);
        decorate(page.getContent()); return page;
    }

    @Transactional(readOnly = true)
    public Product findActiveProduct(Long id) {
        Product product=productRepository.findByIdAndStatusTrueAndCategoryStatusTrue(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        decorate(List.of(product)); return product;
    }

    @Transactional(readOnly = true)
    public List<Product> findNewestProducts() {
        return decorate(productRepository.findTop3ByStatusTrueAndCategoryStatusTrueOrderByCreatedAtDesc());
    }

    @Transactional(readOnly = true)
    public List<Product> findHomepageProducts() {
        return decorate(productRepository.findTop8ByStatusTrueAndCategoryStatusTrueOrderByCreatedAtDesc());
    }

    /**
     * Samples up to eight products per active category from the whole catalog.
     * Categories and their products are shuffled per request, then interleaved
     * so the initial eight cards do not all come from the newest category.
     * Category tabs reuse this bounded, de-duplicated catalog locally.
     */
    @Transactional(readOnly = true)
    public List<Product> findHomepageCatalog(List<Category> categories) {
        if (categories.isEmpty()) {
            return List.of();
        }
        return decorate(mixHomepageCatalog(productRepository.findByStatusTrueAndCategoryStatusTrue(),
            categories, ThreadLocalRandom.current()));
    }

    static List<Product> mixHomepageCatalog(List<Product> candidates, List<Category> categories,
                                           Random random) {
        Map<Long, List<Product>> byCategory = new LinkedHashMap<>();
        categories.stream().filter(Category::isStatus).forEach(category ->
            byCategory.putIfAbsent(category.getId(), new ArrayList<>()));
        Set<Long> seen = new HashSet<>();
        for (Product product : candidates) {
            if (!product.isStatus() || product.getCategory() == null
                || !product.getCategory().isStatus()) {
                continue;
            }
            List<Product> group = byCategory.get(product.getCategory().getId());
            if (group != null && seen.add(product.getId())) {
                group.add(product);
            }
        }

        List<List<Product>> groups = new ArrayList<>();
        for (List<Product> group : byCategory.values()) {
            if (!group.isEmpty()) {
                Collections.shuffle(group, random);
                groups.add(group.subList(0, Math.min(8, group.size())));
            }
        }
        Collections.shuffle(groups, random);

        List<Product> result = new ArrayList<>();
        for (int position = 0; position < 8; position++) {
            for (List<Product> group : groups) {
                if (position < group.size()) {
                    result.add(group.get(position));
                }
            }
        }
        return List.copyOf(result);
    }

    @Transactional(readOnly = true)
    public List<Product> findRelatedProducts(Product product) {
        return decorate(productRepository
            .findTop4ByStatusTrueAndCategoryStatusTrueAndCategoryIdAndIdNotOrderByCreatedAtDesc(
                product.getCategory().getId(),
                product.getId()
            ));
    }

    @Transactional(readOnly = true)
    public List<Product> findSameBrandProducts(Product product) {
        if (product.getBrand() == null || !product.getBrand().isStatus()) {
            return List.of();
        }
        return decorate(productRepository
            .findTop4ByStatusTrueAndCategoryStatusTrueAndBrandIdAndIdNotOrderByCreatedAtDesc(
                product.getBrand().getId(), product.getId()));
    }

    public List<String> galleryImages(Product product) {
        List<String> images = new java.util.ArrayList<>();
        if (product.getImage() != null && !product.getImage().isBlank()) {
            images.add(product.getImage().trim());
        }
        product.getAdditionalImages().stream()
            .map(image -> image.getImageUrl())
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(url -> !url.isEmpty())
            .filter(url -> !images.contains(url))
            .forEach(images::add);
        return images.isEmpty() ? List.of("/img/hero-img.jpg") : List.copyOf(images);
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
        return decorate(ids.stream().map(products::get).filter(product -> product != null).toList());
    }

    private <T extends List<Product>> T decorate(T products) {
        if (promotionPricingService != null) promotionPricingService.decorate(products);
        return products;
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
