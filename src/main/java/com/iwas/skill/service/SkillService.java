package com.iwas.skill.service;

import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import com.iwas.common.storage.StorageService;
import com.iwas.project.entity.Project;
import com.iwas.project.entity.ProjectMember;
import com.iwas.project.repository.ProjectMemberRepository;
import com.iwas.project.repository.ProjectRepository;
import com.iwas.skill.dto.*;
import com.iwas.skill.entity.EmployeeSkill;
import com.iwas.skill.entity.Skill;
import com.iwas.skill.entity.SkillCategory;
import com.iwas.skill.enums.SkillLevel;
import com.iwas.skill.repository.EmployeeSkillRepository;
import com.iwas.skill.repository.SkillCategoryRepository;
import com.iwas.skill.repository.SkillRepository;
import com.iwas.task.repository.TaskSkillRequirementRepository;
import com.iwas.user.entity.User;
import com.iwas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final SkillCategoryRepository skillCategoryRepository;
    private final SkillCategoryService skillCategoryService;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskSkillRequirementRepository taskSkillRequirementRepository;
    private final StorageService storageService;

    public List<SkillResponse> getAllSkills(String keyword, Long categoryId) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        List<Skill> skills;
        if (kw == null && categoryId == null) {
            skills = skillRepository.findAllActive();
        } else if (kw == null) {
            skills = skillRepository.findByCategoryId(categoryId);
        } else if (categoryId == null) {
            skills = skillRepository.searchByKeyword(kw);
        } else {
            skills = skillRepository.searchByKeywordAndCategoryId(kw, categoryId);
        }
        Map<Long, SkillCategory> categories = resolveCategories(skills);
        return skills.stream()
                .map(s -> toSkillResponse(s, categories))
                .toList();
    }

    public SkillResponse getSkillById(Long id) {
        Skill skill = findSkill(id);
        return toSkillResponse(skill, resolveCategories(List.of(skill)));
    }

    @Transactional
    public SkillResponse createSkill(SkillRequest request) {
        skillRepository.findByNameAndIsDeletedFalse(request.getName())
                .ifPresent(s -> { throw new AppException(ErrorCode.SKILL_ALREADY_EXISTS); });

        SkillCategory category = skillCategoryService.findActive(request.getCategoryId());

        Skill skill = new Skill();
        skill.setName(request.getName().trim());
        skill.setCategoryId(category.getId());
        skill.setDescription(request.getDescription());
        Skill saved = skillRepository.save(skill);
        return toSkillResponse(saved, Map.of(category.getId(), category));
    }

    @Transactional
    public SkillResponse updateSkill(Long id, SkillRequest request) {
        Skill skill = findSkill(id);
        SkillCategory category = skillCategoryService.findActive(request.getCategoryId());

        skill.setName(request.getName().trim());
        skill.setCategoryId(category.getId());
        skill.setDescription(request.getDescription());
        Skill saved = skillRepository.save(skill);
        return toSkillResponse(saved, Map.of(category.getId(), category));
    }

    @Transactional
    public void deleteSkill(Long id) {
        Skill skill = findSkill(id);

        if (taskSkillRequirementRepository.countActiveTaskRequirements(id) > 0) {
            throw new AppException(ErrorCode.SKILL_IN_USE_BY_TASKS);
        }
        if (employeeSkillRepository.countActiveBySkillId(id) > 0) {
            throw new AppException(ErrorCode.SKILL_IN_USE_BY_MEMBERS);
        }

        skill.setIsDeleted(true);
        skillRepository.save(skill);
    }

    public List<EmployeeSkillResponse> getEmployeeSkills(Long userId) {
        List<EmployeeSkill> employeeSkills = employeeSkillRepository.findByUserId(userId);
        List<Skill> skills = skillRepository.findAllById(
                employeeSkills.stream().map(EmployeeSkill::getSkillId).toList()
        );
        Map<Long, Skill> skillMap = skills.stream().collect(Collectors.toMap(Skill::getId, s -> s));
        Map<Long, SkillCategory> categories = resolveCategories(skills);

        return employeeSkills.stream()
                .map(es -> toEmployeeSkillResponse(es, skillMap.get(es.getSkillId()), categories))
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
        employeeSkill.setNote(request.getNote());
        EmployeeSkill saved = employeeSkillRepository.save(employeeSkill);
        return toEmployeeSkillResponse(saved, skill, resolveCategories(List.of(skill)));
    }

    @Transactional
    public EmployeeSkillResponse updateEmployeeSkill(Long userId, Long employeeSkillId, EmployeeSkillRequest request) {
        EmployeeSkill employeeSkill = employeeSkillRepository.findById(employeeSkillId)
                .filter(es -> es.getUserId().equals(userId) && !Boolean.TRUE.equals(es.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_SKILL_NOT_FOUND));

        employeeSkill.setLevel(request.getLevel());
        employeeSkill.setNote(request.getNote());

        Skill skill = findSkill(employeeSkill.getSkillId());
        EmployeeSkill saved = employeeSkillRepository.save(employeeSkill);
        return toEmployeeSkillResponse(saved, skill, resolveCategories(List.of(skill)));
    }

    @Transactional
    public void removeEmployeeSkill(Long userId, Long employeeSkillId) {
        EmployeeSkill employeeSkill = employeeSkillRepository.findById(employeeSkillId)
                .filter(es -> es.getUserId().equals(userId) && !Boolean.TRUE.equals(es.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_SKILL_NOT_FOUND));
        employeeSkill.setIsDeleted(true);
        employeeSkillRepository.save(employeeSkill);
    }

    public List<SkillMemberResponse> getMembersBySkill(Long skillId, SkillLevel minLevel, Long excludeProjectId) {
        Skill skill = findSkill(skillId);

        List<SkillLevel> acceptedLevels = (minLevel == null)
                ? Arrays.asList(SkillLevel.values())
                : Arrays.stream(SkillLevel.values())
                        .filter(l -> l.ordinal() >= minLevel.ordinal())
                        .toList();

        List<EmployeeSkill> employeeSkills = employeeSkillRepository.findBySkillIdAndLevelIn(skillId, acceptedLevels);

        Set<Long> excludedUserIds = excludeProjectId == null
                ? Set.of()
                : collectProjectParticipantIds(excludeProjectId);

        Map<Long, EmployeeSkill> esByUser = employeeSkills.stream()
                .filter(es -> !excludedUserIds.contains(es.getUserId()))
                .collect(Collectors.toMap(EmployeeSkill::getUserId, es -> es, (a, b) -> a));

        if (esByUser.isEmpty()) {
            return List.of();
        }

        Map<Long, User> userMap = userRepository.findAllById(esByUser.keySet()).stream()
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()) && Boolean.TRUE.equals(u.getIsActive()))
                .collect(Collectors.toMap(User::getId, u -> u));

        return esByUser.values().stream()
                .filter(es -> userMap.containsKey(es.getUserId()))
                .sorted(Comparator
                        .<EmployeeSkill, Integer>comparing(es -> -es.getLevel().ordinal())
                        .thenComparing(es -> userMap.get(es.getUserId()).getFullName(),
                                Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(es -> toSkillMemberResponse(userMap.get(es.getUserId()), es, skill))
                .toList();
    }

    public SkillStatsResponse getSkillStats(Long skillId) {
        Skill skill = findSkill(skillId);
        List<EmployeeSkill> employeeSkills = employeeSkillRepository.findBySkillId(skillId);

        Map<SkillLevel, Long> distribution = new EnumMap<>(SkillLevel.class);
        for (SkillLevel level : SkillLevel.values()) {
            distribution.put(level, 0L);
        }
        employeeSkills.forEach(es -> distribution.merge(es.getLevel(), 1L, Long::sum));

        String categoryName = (skill.getCategoryId() == null) ? null :
                skillCategoryRepository.findById(skill.getCategoryId())
                        .map(SkillCategory::getName)
                        .orElse(null);

        return SkillStatsResponse.builder()
                .skillId(skill.getId())
                .skillName(skill.getName())
                .skillCategory(categoryName)
                .memberCount(employeeSkills.size())
                .levelDistribution(distribution)
                .openTaskRequirementCount(taskSkillRequirementRepository.countOpenTaskRequirements(skillId))
                .build();
    }

    private Set<Long> collectProjectParticipantIds(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        Set<Long> ids = new HashSet<>();
        if (project.getManagerId() != null) {
            ids.add(project.getManagerId());
        }
        projectMemberRepository.findActiveMembersByProjectId(projectId).stream()
                .map(ProjectMember::getUserId)
                .forEach(ids::add);
        return ids;
    }

    private Map<Long, SkillCategory> resolveCategories(Collection<Skill> skills) {
        Set<Long> categoryIds = skills.stream()
                .map(Skill::getCategoryId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) return Map.of();
        return skillCategoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(SkillCategory::getId, c -> c));
    }

    private SkillMemberResponse toSkillMemberResponse(User user, EmployeeSkill es, Skill skill) {
        return SkillMemberResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(storageService.resolveUrl(user.getAvatarId()))
                .position(user.getPosition())
                .role(user.getRole())
                .level(es.getLevel())
                .note(es.getNote())
                .build();
    }

    private Skill findSkill(Long id) {
        return skillRepository.findById(id)
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.SKILL_NOT_FOUND));
    }

    private SkillResponse toSkillResponse(Skill skill, Map<Long, SkillCategory> categories) {
        SkillCategory cat = (skill.getCategoryId() == null) ? null : categories.get(skill.getCategoryId());
        return SkillResponse.builder()
                .id(skill.getId())
                .name(skill.getName())
                .categoryId(skill.getCategoryId())
                .categoryName(cat == null ? null : cat.getName())
                .description(skill.getDescription())
                .build();
    }

    private EmployeeSkillResponse toEmployeeSkillResponse(EmployeeSkill es, Skill skill, Map<Long, SkillCategory> categories) {
        SkillCategory cat = (skill == null || skill.getCategoryId() == null) ? null : categories.get(skill.getCategoryId());
        return EmployeeSkillResponse.builder()
                .id(es.getId())
                .userId(es.getUserId())
                .skillId(es.getSkillId())
                .skillName(skill != null ? skill.getName() : null)
                .skillCategory(cat == null ? null : cat.getName())
                .level(es.getLevel())
                .note(es.getNote())
                .build();
    }
}
