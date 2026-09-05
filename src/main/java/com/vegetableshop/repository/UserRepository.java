package com.vegetableshop.repository;

import com.vegetableshop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByOauthSubject(String oauthSubject);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
        select u from User u
        where :keyword = ''
           or lower(u.fullName) like lower(concat('%', :keyword, '%'))
           or lower(u.email) like lower(concat('%', :keyword, '%'))
        """)
    Page<User> searchForAdmin(@Param("keyword") String keyword, Pageable pageable);
}
