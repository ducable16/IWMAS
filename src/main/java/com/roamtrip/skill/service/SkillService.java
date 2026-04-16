package com.roamtrip.skill.service;

import com.roamtrip.common.enums.ErrorCode;
import com.roamtrip.common.exception.AppException;
import com.roamtrip.skill.dto.*;
import com.roamtrip.skill.entity.EmployeeSkill;
import com.roamtrip.skill.entity.Skill;
import com.roamtrip.skill.repository.EmployeeSkillRepository;
import com.roamtrip.skill.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;
    private final EmployeeSkillRepository employeeSkillRepository;

    public List<SkillResponse> getAllSkills() {
        return skillRepository.findAllActive().stream()
                .map(this::toSkillResponse)
                .toList();
    }

    public SkillResponse getSkillById(Long id) {
        return toSkillResponse(findSkill(id));
    }

    @Transactional
    public SkillResponse createSkill(SkillRequest request) {
        skillRepository.findByNameAndIsDeletedFalse(request.getName())
                .ifPresent(s -> { throw new AppException(ErrorCode.SKILL_ALREADY_EXISTS); });

        Skill skill = new Skill();
        skill.setName(request.getName().trim());
        skill.setCategory(request.getCategory());
        skill.setDescription(request.getDescription());
        return toSkillResponse(skillRepository.save(skill));
    }

    @Transactional
    public SkillResponse updateSkill(Long id, SkillRequest request) {
        Skill skill = findSkill(id);
        skill.setName(request.getName().trim());
        skill.setCategory(request.getCategory());
        skill.setDescription(request.getDescription());
        return toSkillResponse(skillRepository.save(skill));
    }

    @Transactional
    public void deleteSkill(Long id) {
        Skill skill = findSkill(id);
        skill.setIsDeleted(true);
        skillRepository.save(skill);
    }

    public List<EmployeeSkillResponse> getEmployeeSkills(Long userId) {
        List<EmployeeSkill> employeeSkills = employeeSkillRepository.findByUserId(userId);
        Map<Long, Skill> skillMap = skillRepository.findAllById(
                employeeSkills.stream().map(EmployeeSkill::getSkillId).toList()
        ).stream().collect(Collectors.toMap(Skill::getId, s -> s));

        return employeeSkills.stream()
                .map(es -> toEmployeeSkillResponse(es, skillMap.get(es.getSkillId())))
                .toList();
    }

    @Transactional
    public EmployeeSkillResponse addEmployeeSkill(Long userId, EmployeeSkillRequest request) {
        employeeSkillRepository.findByUserIdAndSkillIdAndIsDeletedFalse(userId, request.getSkillId())
                .ifPresent(es -> { throw new AppException(ErrorCode.EMPLOYEE_SKILL_ALREADY_EXISTS); });

        Skill skill = findSkill(request.getSkillId());
        EmployeeSkill employeeSkill = new EmployeeSkill();
        employeeSkill.setUserId(userId);
        employeeSkill.setSkillId(request.getSkillId());
        employeeSkill.setLevel(request.getLevel());
        employeeSkill.setYearsOfExperience(request.getYearsOfExperience());
        employeeSkill.setNote(request.getNote());
        return toEmployeeSkillResponse(employeeSkillRepository.save(employeeSkill), skill);
    }

    @Transactional
    public EmployeeSkillResponse updateEmployeeSkill(Long userId, Long employeeSkillId, EmployeeSkillRequest request) {
        EmployeeSkill employeeSkill = employeeSkillRepository.findById(employeeSkillId)
                .filter(es -> es.getUserId().equals(userId) && !Boolean.TRUE.equals(es.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_SKILL_NOT_FOUND));

        employeeSkill.setLevel(request.getLevel());
        employeeSkill.setYearsOfExperience(request.getYearsOfExperience());
        employeeSkill.setNote(request.getNote());

        Skill skill = findSkill(employeeSkill.getSkillId());
        return toEmployeeSkillResponse(employeeSkillRepository.save(employeeSkill), skill);
    }

    @Transactional
    public void removeEmployeeSkill(Long userId, Long employeeSkillId) {
        EmployeeSkill employeeSkill = employeeSkillRepository.findById(employeeSkillId)
                .filter(es -> es.getUserId().equals(userId) && !Boolean.TRUE.equals(es.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_SKILL_NOT_FOUND));
        employeeSkill.setIsDeleted(true);
        employeeSkillRepository.save(employeeSkill);
    }

    private Skill findSkill(Long id) {
        return skillRepository.findById(id)
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.SKILL_NOT_FOUND));
    }

    private SkillResponse toSkillResponse(Skill skill) {
        return SkillResponse.builder()
                .id(skill.getId())
                .name(skill.getName())
                .category(skill.getCategory())
                .description(skill.getDescription())
                .build();
    }

    private EmployeeSkillResponse toEmployeeSkillResponse(EmployeeSkill es, Skill skill) {
        return EmployeeSkillResponse.builder()
                .id(es.getId())
                .userId(es.getUserId())
                .skillId(es.getSkillId())
                .skillName(skill != null ? skill.getName() : null)
                .skillCategory(skill != null ? skill.getCategory() : null)
                .level(es.getLevel())
                .yearsOfExperience(es.getYearsOfExperience())
                .note(es.getNote())
                .build();
    }
}
