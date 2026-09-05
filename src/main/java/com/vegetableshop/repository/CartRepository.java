package com.vegetableshop.repository;

import com.vegetableshop.entity.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Cart c where lower(c.user.email) = lower(:email)")
    Optional<Cart> findForCheckout(@org.springframework.data.repository.query.Param("email") String email);

    @EntityGraph(attributePaths = {"items", "items.product", "items.product.category", "items.product.brand"})
    Optional<Cart> findByUserEmailIgnoreCase(String email);
}
