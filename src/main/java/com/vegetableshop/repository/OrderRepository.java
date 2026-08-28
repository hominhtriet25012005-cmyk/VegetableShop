package com.vegetableshop.repository;

import com.vegetableshop.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.vegetableshop.entity.OrderStatus;

import java.math.BigDecimal;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"details"})
    List<Order> findByUserEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    @EntityGraph(attributePaths = {"user", "details", "details.product"})
    Optional<Order> findByIdAndUserEmailIgnoreCase(Long id, String email);

    @EntityGraph(attributePaths = {"user"})
    @Query("""
        select o from Order o join o.user u
        where (:keyword = ''
               or lower(o.orderCode) like lower(concat('%', :keyword, '%'))
               or lower(u.fullName) like lower(concat('%', :keyword, '%'))
               or lower(u.email) like lower(concat('%', :keyword, '%')))
          and (:status is null or o.status = :status)
        """)
    Page<Order> searchForAdmin(
        @Param("keyword") String keyword,
        @Param("status") OrderStatus status,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"user", "details", "details.product"})
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findAdminById(@Param("id") Long id);

    @EntityGraph(attributePaths = "user")
    List<Order> findTop5ByOrderByCreatedAtDesc();

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o where o.status = :status")
    BigDecimal calculateRevenueByStatus(@Param("status") OrderStatus status);

    @Query("""
        select (count(d) > 0) from OrderDetail d
        join d.order o join o.user u
        where lower(u.email) = lower(:email)
          and d.product.id = :productId
          and o.status = com.vegetableshop.entity.OrderStatus.COMPLETED
        """)
    boolean hasCompletedPurchase(
        @Param("email") String email,
        @Param("productId") Long productId
    );
}
