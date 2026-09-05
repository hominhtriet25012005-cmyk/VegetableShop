package com.vegetableshop.repository;

import com.vegetableshop.entity.OrderDetail;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from OrderDetail d where d.id = :id")
    Optional<OrderDetail> lockForReview(@Param("id") Long id);

    @EntityGraph(attributePaths = {"order"})
    @Query("""
        select d from OrderDetail d where lower(d.order.user.email) = lower(:email)
          and d.order.user.status = true and d.product.id = :productId
          and d.order.status = com.vegetableshop.entity.OrderStatus.COMPLETED
          and not exists (select r.id from Review r where r.orderDetail.id = d.id)
        order by d.id desc
        """)
    List<OrderDetail> findReviewable(@Param("email") String email, @Param("productId") Long productId);
}
