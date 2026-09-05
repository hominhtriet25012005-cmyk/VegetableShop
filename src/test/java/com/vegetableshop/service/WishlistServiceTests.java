package com.vegetableshop.service;

import com.vegetableshop.entity.Category;
import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.User;
import com.vegetableshop.entity.Wishlist;
import com.vegetableshop.repository.ProductRepository;
import com.vegetableshop.repository.UserRepository;
import com.vegetableshop.repository.WishlistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTests {

    @Mock WishlistRepository wishlistRepository;
    @Mock UserRepository userRepository;
    @Mock ProductRepository productRepository;

    @Test
    void addDoesNotCreateDuplicateForSameUserAndProduct() {
        User user = activeUser();
        Product product = activeProduct();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(productRepository.findByIdAndStatusTrueAndCategoryStatusTrue(3L)).thenReturn(Optional.of(product));
        when(wishlistRepository.findByUserIdAndProductId(1L, 3L)).thenReturn(Optional.of(new Wishlist()));

        assertFalse(service().add("user@example.com", 3L));
        verify(wishlistRepository, never()).saveAndFlush(any());
    }

    @Test
    void addCreatesWishlistWhenItDoesNotExist() {
        User user = activeUser();
        Product product = activeProduct();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(productRepository.findByIdAndStatusTrueAndCategoryStatusTrue(3L)).thenReturn(Optional.of(product));
        when(wishlistRepository.findByUserIdAndProductId(1L, 3L)).thenReturn(Optional.empty());

        assertTrue(service().add("user@example.com", 3L));
        verify(wishlistRepository).saveAndFlush(any(Wishlist.class));
    }

    @Test
    void inactiveProductsAreNotShownOnWishlistPage() {
        Wishlist visible = new Wishlist();
        visible.setProduct(activeProduct());
        Wishlist hidden = new Wishlist();
        Product inactive = activeProduct();
        inactive.setStatus(false);
        hidden.setProduct(inactive);
        when(wishlistRepository.findByUserEmailIgnoreCaseOrderByCreatedAtDesc("user@example.com"))
            .thenReturn(List.of(visible, hidden));

        assertEquals(List.of(visible), service().findActiveItems("user@example.com"));
    }

    private WishlistService service() {
        return new WishlistService(wishlistRepository, userRepository, productRepository);
    }

    private User activeUser() {
        User user = new User();
        user.setId(1L);
        user.setStatus(true);
        return user;
    }

    private Product activeProduct() {
        Category category = new Category();
        category.setId(2L);
        category.setStatus(true);
        Product product = new Product();
        product.setId(3L);
        product.setCategory(category);
        product.setStatus(true);
        return product;
    }
}
