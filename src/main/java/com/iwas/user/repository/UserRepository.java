package com.iwas.user.repository;

import com.iwas.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.isDeleted = false ORDER BY u.fullName")
    List<User> findAllActiveUsers();

    @Query("SELECT u FROM User u WHERE u.id IN :userIds AND u.isDeleted = false " +
           "AND (LOWER(u.fullName) LIKE :keyword OR LOWER(u.email) LIKE :keyword " +
           "OR LOWER(u.position) LIKE :keyword) ORDER BY u.fullName")
    List<User> searchByIdsAndKeyword(@Param("userIds") List<Long> userIds,
                                     @Param("keyword") String keyword,
                                     Pageable pageable);
}
