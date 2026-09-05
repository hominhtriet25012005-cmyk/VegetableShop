package com.vegetableshop.service;

import com.vegetableshop.entity.Category;
import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.ProductViewHistory;
import com.vegetableshop.entity.User;
import com.vegetableshop.repository.ProductRepository;
import com.vegetableshop.repository.ProductViewHistoryRepository;
import com.vegetableshop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductViewHistoryServiceTests {

    @Mock ProductViewHistoryRepository historyRepository;
    @Mock UserRepository userRepository;
    @Mock ProductRepository productRepository;

    @Test
    void repeatedViewIncrementsCounterAndReturnsOnlyActivePreviousProducts() {
        User user = new User();
        user.setId(1L);
        user.setStatus(true);
        Product current = product(4L, true);
        Product previous = product(3L, true);
        Product inactive = product(2L, false);
        ProductViewHistory currentHistory = history(8L, user, current, 2);
        ProductViewHistory previousHistory = history(7L, user, previous, 1);
        ProductViewHistory inactiveHistory = history(6L, user, inactive, 1);

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(productRepository.findByIdAndStatusTrueAndCategoryStatusTrue(4L)).thenReturn(Optional.of(current));
        when(historyRepository.findByUserIdAndProductId(1L, 4L)).thenReturn(Optional.of(currentHistory));
        when(historyRepository.findTop6ByUserEmailIgnoreCaseAndProductIdNotOrderByUpdatedAtDesc(
            "user@example.com", 4L)).thenReturn(List.of(previousHistory, inactiveHistory));

        List<Product> result = service().recordAndFindPrevious("user@example.com", 4L);

        assertEquals(3, currentHistory.getViewCount());
        assertEquals(List.of(previous), result);
        verify(historyRepository).saveAndFlush(currentHistory);
    }

    private ProductViewHistoryService service() {
        return new ProductViewHistoryService(historyRepository, userRepository, productRepository);
    }

    private Product product(Long id, boolean active) {
        Category category = new Category();
        category.setId(1L);
        category.setStatus(true);
        Product product = new Product();
        product.setId(id);
        product.setStatus(active);
        product.setCategory(category);
        return product;
    }

    private ProductViewHistory history(Long id, User user, Product product, int count) {
        ProductViewHistory history = new ProductViewHistory();
        history.setId(id);
        history.setUser(user);
        history.setProduct(product);
        history.setViewCount(count);
        return history;
    }
}
