package com.vegetableshop.repository;

import com.vegetableshop.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.math.BigDecimal;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @EntityGraph(attributePaths = "product")
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    @EntityGraph(attributePaths = "product")
    Optional<CartItem> findByIdAndCartUserEmailIgnoreCase(Long id, String email);

    @Query("""
        select coalesce(sum(item.quantity), 0)
        from CartItem item
        where lower(item.cart.user.email) = lower(:email)
        """)
    Long sumQuantityByUserEmail(@Param("email") String email);

    @Query("""
        select coalesce(sum(item.product.price * item.quantity), 0)
        from CartItem item
        where lower(item.cart.user.email) = lower(:email)
        """)
    BigDecimal sumSubtotalByUserEmail(@Param("email") String email);

    long countByCartUserEmailIgnoreCase(String email);
}
