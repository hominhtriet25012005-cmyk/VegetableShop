package com.vegetableshop.repository;

import com.vegetableshop.entity.AccountActivationToken;
import com.vegetableshop.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountActivationTokenRepository extends JpaRepository<AccountActivationToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from AccountActivationToken token where token.tokenHash = :tokenHash")
    Optional<AccountActivationToken> findForUpdateByTokenHash(@Param("tokenHash") String tokenHash);

    List<AccountActivationToken> findAllByUserAndUsedAtIsNull(User user);
}
