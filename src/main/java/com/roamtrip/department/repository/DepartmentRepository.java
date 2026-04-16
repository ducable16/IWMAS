package com.roamtrip.department.repository;

import com.roamtrip.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    @Query("SELECT d FROM Department d WHERE d.isDeleted = false ORDER BY d.name")
    List<Department> findAllActive();

    Optional<Department> findByNameAndIsDeletedFalse(String name);
}
