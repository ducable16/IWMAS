package com.iwas.department.service;

import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import com.iwas.department.dto.DepartmentRequest;
import com.iwas.department.dto.DepartmentResponse;
import com.iwas.department.entity.Department;
import com.iwas.department.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public List<DepartmentResponse> getAll() {
        return departmentRepository.findAllActive().stream()
                .map(this::toResponse)
                .toList();
    }

    public DepartmentResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        departmentRepository.findByNameAndIsDeletedFalse(request.getName())
                .ifPresent(d -> { throw new AppException(ErrorCode.DEPARTMENT_ALREADY_EXISTS); });

        Department department = new Department();
        department.setName(request.getName().trim());
        department.setDescription(request.getDescription());
        department.setManagerId(request.getManagerId());
        return toResponse(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = findById(id);
        department.setName(request.getName().trim());
        department.setDescription(request.getDescription());
        department.setManagerId(request.getManagerId());
        return toResponse(departmentRepository.save(department));
    }

    @Transactional
    public void delete(Long id) {
        Department department = findById(id);
        department.setIsDeleted(true);
        departmentRepository.save(department);
    }

    private Department findById(Long id) {
        return departmentRepository.findById(id)
                .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));
    }

    private DepartmentResponse toResponse(Department d) {
        return DepartmentResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .description(d.getDescription())
                .managerId(d.getManagerId())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
