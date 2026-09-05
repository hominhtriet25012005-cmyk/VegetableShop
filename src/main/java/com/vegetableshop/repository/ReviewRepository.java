package com.vegetableshop.repository;

import com.vegetableshop.entity.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import com.vegetableshop.entity.ReviewStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {"user", "images", "orderDetail"})
    @Query("""
        select r from Review r where r.product.id = :productId
          and r.status = com.vegetableshop.entity.ReviewStatus.APPROVED and r.orderDetail is not null
        order by r.createdAt desc, r.id desc
        """)
    List<Review> findPublished(@Param("productId") Long productId);

    @EntityGraph(attributePaths = {"images", "orderDetail", "orderDetail.order"})
    List<Review> findByUserEmailIgnoreCaseAndProductIdOrderByCreatedAtDesc(String email, Long productId);
    boolean existsByOrderDetailId(Long orderDetailId);

    @Query("""
        select r.rating, count(r) from Review r where r.product.id = :productId
          and r.status = com.vegetableshop.entity.ReviewStatus.APPROVED and r.orderDetail is not null
        group by r.rating
        """)
    List<Object[]> countPublishedByRating(@Param("productId") Long productId);

    @EntityGraph(attributePaths = {"user", "product", "orderDetail"})
    @Query("""
        select r from Review r where (:status is null or r.status = :status)
          and (:keyword = '' or lower(r.product.name) like lower(concat('%', :keyword, '%'))
              or lower(r.user.fullName) like lower(concat('%', :keyword, '%')))
        """)
    Page<Review> searchAdmin(@Param("keyword") String keyword, @Param("status") ReviewStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "product", "images", "orderDetail", "orderDetail.order", "moderatedBy"})
    @Query("select r from Review r where r.id = :id")
    Optional<Review> findAdminDetail(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Review r where r.id = :id")
    Optional<Review> lockById(@Param("id") Long id);
}
