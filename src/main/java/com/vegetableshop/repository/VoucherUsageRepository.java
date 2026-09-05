package com.vegetableshop.repository;

import com.vegetableshop.entity.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {
    long countByVoucherIdAndActiveTrue(Long voucherId);
    long countByVoucherIdAndUserIdAndActiveTrue(Long voucherId, Long userId);
    Optional<VoucherUsage> findByOrderIdAndActiveTrue(Long orderId);
}
