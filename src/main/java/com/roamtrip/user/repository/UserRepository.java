package com.roamtrip.user.repository;

import com.roamtrip.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.isDeleted = false ORDER BY u.fullName")
    List<User> findAllActiveUsers();

    @Query("SELECT u FROM User u WHERE u.isDeleted = false AND u.departmentId = :departmentId")
    List<User> findByDepartmentId(Long departmentId);
}
