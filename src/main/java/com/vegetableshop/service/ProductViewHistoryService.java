package com.vegetableshop.service;

import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.ProductViewHistory;
import com.vegetableshop.entity.User;
import com.vegetableshop.repository.ProductRepository;
import com.vegetableshop.repository.ProductViewHistoryRepository;
import com.vegetableshop.repository.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Profile("mysql")
public class ProductViewHistoryService {

    private final ProductViewHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public ProductViewHistoryService(
        ProductViewHistoryRepository historyRepository,
        UserRepository userRepository,
        ProductRepository productRepository
    ) {
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public List<Product> recordAndFindPrevious(String email, Long productId) {
        User user = userRepository.findByEmailIgnoreCase(email)
            .filter(User::isStatus)
            .orElse(null);
        if (user == null) {
            return List.of();
        }
        Product product = productRepository.findByIdAndStatusTrueAndCategoryStatusTrue(productId)
            .orElse(null);
        if (product == null) {
            return List.of();
        }

        ProductViewHistory history = historyRepository.findByUserIdAndProductId(user.getId(), productId)
            .orElseGet(() -> newHistory(user, product));
        if (history.getId() != null) {
            history.setViewCount(history.getViewCount() + 1);
        }
        historyRepository.saveAndFlush(history);

        return historyRepository
            .findTop6ByUserEmailIgnoreCaseAndProductIdNotOrderByUpdatedAtDesc(email, productId)
            .stream()
            .map(ProductViewHistory::getProduct)
            .filter(item -> item.isStatus() && item.getCategory().isStatus())
            .toList();
    }

    private ProductViewHistory newHistory(User user, Product product) {
        ProductViewHistory history = new ProductViewHistory();
        history.setUser(user);
        history.setProduct(product);
        return history;
    }
}
