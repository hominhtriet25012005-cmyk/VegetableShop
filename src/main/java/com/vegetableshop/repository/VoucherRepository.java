package com.vegetableshop.repository;

import com.vegetableshop.entity.Voucher;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    @EntityGraph(attributePaths = "scopes")
    Optional<Voucher> findByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "scopes")
    @Query("select v from Voucher v where lower(v.code)=lower(:code)")
    Optional<Voucher> findByCodeForUpdate(@Param("code") String code);

    @EntityGraph(attributePaths = "scopes")
    List<Voucher> findAllByOrderByCreatedAtDesc();
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
