package com.iwas.skill.service;

import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import com.iwas.skill.dto.SkillCategoryRequest;
import com.iwas.skill.dto.SkillCategoryResponse;
import com.iwas.skill.entity.SkillCategory;
import com.iwas.skill.repository.SkillCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillCategoryService {

    private final SkillCategoryRepository skillCategoryRepository;

    public List<SkillCategoryResponse> getAll() {
        return skillCategoryRepository.findAllActive().stream()
                .map(this::toResponse)
                .toList();
    }

    public SkillCategoryResponse getById(Long id) {
        return toResponse(findActive(id));
    }

    @Transactional
    public SkillCategoryResponse create(SkillCategoryRequest request) {
        String name = request.getName().trim();
        skillCategoryRepository.findByNameIgnoreCase(name)
                .ifPresent(c -> { throw new AppException(ErrorCode.SKILL_CATEGORY_ALREADY_EXISTS); });

        SkillCategory entity = new SkillCategory();
        entity.setName(name);
        entity.setDescription(request.getDescription());
        return toResponse(skillCategoryRepository.save(entity));
    }

    @Transactional
    public SkillCategoryResponse update(Long id, SkillCategoryRequest request) {
        SkillCategory entity = findActive(id);
        String newName = request.getName().trim();
        skillCategoryRepository.findByNameIgnoreCase(newName)
                .filter(c -> !c.getId().equals(id))
                .ifPresent(c -> { throw new AppException(ErrorCode.SKILL_CATEGORY_ALREADY_EXISTS); });

        entity.setName(newName);
        entity.setDescription(request.getDescription());
        return toResponse(skillCategoryRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        SkillCategory entity = findActive(id);
        if (skillCategoryRepository.countActiveSkillsByCategory(id) > 0) {
            throw new AppException(ErrorCode.SKILL_CATEGORY_IN_USE);
        }
        entity.setIsDeleted(true);
        skillCategoryRepository.save(entity);
    }

    SkillCategory findActive(Long id) {
        return skillCategoryRepository.findById(id)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.SKILL_CATEGORY_NOT_FOUND));
    }

    private SkillCategoryResponse toResponse(SkillCategory c) {
        return SkillCategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .build();
    }
}
