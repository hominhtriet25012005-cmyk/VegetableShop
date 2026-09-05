package com.vegetableshop.repository;
import com.vegetableshop.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface BankTransferPaymentRepository extends JpaRepository<BankTransferPayment,Long> {
    @EntityGraph(attributePaths = {"order", "order.user"})
    Optional<BankTransferPayment> findByOrderId(Long orderId);
    @EntityGraph(attributePaths = {"order", "order.user"})
    @Query("select p from BankTransferPayment p where (:status is null or p.order.paymentStatus = :status)")
    Page<BankTransferPayment> search(@Param("status") PaymentStatus status, Pageable pageable);
}
