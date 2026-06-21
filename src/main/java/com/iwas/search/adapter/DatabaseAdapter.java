package com.iwas.search.adapter;

import com.iwas.project.entity.Project;
import com.iwas.project.repository.ProjectRepository;
import com.iwas.search.config.SearchProperties;
import com.iwas.search.dto.ProjectSearchResult;
import com.iwas.search.dto.SearchRequest;
import com.iwas.search.dto.SearchResponse;
import com.iwas.search.dto.SuggestionItem;
import com.iwas.search.dto.UserSearchResult;
import com.iwas.common.storage.StorageService;
import com.iwas.search.service.SearchFallbackService;
import com.iwas.skill.dto.RequiredSkill;
import com.iwas.skill.entity.EmployeeSkill;
import com.iwas.user.entity.User;
import com.iwas.user.enums.UserRole;
import com.iwas.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseAdapter implements SearchFallbackService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final SearchProperties properties;
    private final StorageService storageService;

    // -------------------------------------------------------------------------
    // User
    // -------------------------------------------------------------------------

    @Override
    public SearchResponse<UserSearchResult> searchUsers(SearchRequest request) {
        long start = System.currentTimeMillis();
        int size = Math.min(request.getSize(), properties.getElasticsearch().getMaxPageSize());
        boolean hasQuery = request.getQuery() != null && !request.getQuery().isBlank();
        String like = hasQuery ? "%" + request.getQuery().toLowerCase() + "%" : null;
        List<RequiredSkill> requiredSkills = request.getRequiredSkills() == null
                ? List.of() : request.getRequiredSkills();

        Specification<User> spec = (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("isDeleted"), false));
            if (hasQuery) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(cb.lower(root.get("position")), like),
                        cb.like(cb.lower(root.get("email")), like)));
            }
            if (request.getRole() != null) {
                predicates.add(cb.equal(root.get("role"), request.getRole()));
            }
            for (RequiredSkill rs : requiredSkills) {
                Subquery<Long> sub = q.subquery(Long.class);
                Root<EmployeeSkill> es = sub.from(EmployeeSkill.class);
                sub.select(es.get("id"));
                sub.where(
                        cb.equal(es.get("userId"), root.get("id")),
                        cb.equal(es.get("skillId"), rs.getSkillId()),
                        cb.equal(es.get("isDeleted"), false),
                        es.get("level").in(rs.acceptedLevels()));
                predicates.add(cb.exists(sub));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageable = PageRequest.of(request.getPage(), size,
                Sort.by(Sort.Direction.ASC, "fullName"));

        Page<User> page = userRepository.findAll(spec, pageable);

        List<UserSearchResult> items = page.getContent().stream()
                .map(this::toUserResult)
                .collect(Collectors.toList());

        return SearchResponse.<UserSearchResult>builder()
                .items(items)
                .total(page.getTotalElements())
                .page(request.getPage())
                .size(size)
                .source("database")
                .tookMs(System.currentTimeMillis() - start)
                .build();
    }

    @Override
    public List<SuggestionItem> autocompleteUsers(String prefix, int topN, UserRole role) {
        SearchRequest req = SearchRequest.builder()
                .query(prefix)
                .role(role)
                .page(0)
                .size(topN)
                .build();
        return searchUsers(req).getItems().stream()
                .map(u -> SuggestionItem.builder()
                        .term(u.getFullName())
                        .entityId(u.getId())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<SuggestionItem> autocompleteUsersExcluding(String prefix, int topN, Set<Long> excludeIds, UserRole role) {
        String like = "%" + prefix.toLowerCase() + "%";

        Specification<User> spec = (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("isDeleted"), false));
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("fullName")), like),
                    cb.like(cb.lower(root.get("position")), like),
                    cb.like(cb.lower(root.get("email")), like)));
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (!excludeIds.isEmpty()) {
                predicates.add(cb.not(root.get("id").in(excludeIds)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageable = PageRequest.of(0, topN, Sort.by(Sort.Direction.ASC, "fullName"));
        return userRepository.findAll(spec, pageable).getContent().stream()
                .map(u -> SuggestionItem.builder()
                        .term(u.getFullName())
                        .entityId(u.getId())
                        .build())
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Project
    // -------------------------------------------------------------------------

    @Override
    public SearchResponse<ProjectSearchResult> searchProjects(SearchRequest request) {
        long start = System.currentTimeMillis();
        int size = Math.min(request.getSize(), properties.getElasticsearch().getMaxPageSize());
        String like = "%" + request.getQuery().toLowerCase() + "%";

        Specification<Project> spec = (root, q, cb) -> cb.and(
                cb.equal(root.get("isDeleted"), false),
                cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("code")), like)));

        PageRequest pageable = PageRequest.of(request.getPage(), size,
                Sort.by(Sort.Direction.ASC, "name"));

        Page<Project> page = projectRepository.findAll(spec, pageable);

        List<ProjectSearchResult> items = page.getContent().stream()
                .map(this::toProjectResult)
                .collect(Collectors.toList());

        return SearchResponse.<ProjectSearchResult>builder()
                .items(items)
                .total(page.getTotalElements())
                .page(request.getPage())
                .size(size)
                .source("database")
                .tookMs(System.currentTimeMillis() - start)
                .build();
    }

    @Override
    public List<SuggestionItem> autocompleteProjects(String prefix, int topN) {
        SearchRequest req = SearchRequest.builder()
                .query(prefix)
                .page(0)
                .size(topN)
                .build();
        return searchProjects(req).getItems().stream()
                .map(p -> SuggestionItem.builder()
                        .term(p.getName())
                        .entityId(p.getId())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<SuggestionItem> autocompleteProjectsWithin(String prefix, int topN, Set<Long> allowedIds) {
        if (allowedIds.isEmpty()) {
            return List.of();
        }
        String like = "%" + prefix.toLowerCase() + "%";
        Specification<Project> spec = (root, q, cb) -> cb.and(
                cb.equal(root.get("isDeleted"), false),
                root.get("id").in(allowedIds),
                cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("code")), like)));

        PageRequest pageable = PageRequest.of(0, topN, Sort.by(Sort.Direction.ASC, "name"));
        return projectRepository.findAll(spec, pageable).getContent().stream()
                .map(p -> SuggestionItem.builder()
                        .term(p.getName())
                        .entityId(p.getId())
                        .build())
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private UserSearchResult toUserResult(User u) {
        return UserSearchResult.builder()
                .id(u.getId())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .position(u.getPosition())
                .avatarUrl(storageService.resolveUrl(u.getAvatarId()))
                .role(u.getRole() == null ? null : u.getRole().name())
                .build();
    }

    private ProjectSearchResult toProjectResult(Project p) {
        return ProjectSearchResult.builder()
                .id(p.getId())
                .name(p.getName())
                .code(p.getCode())
                .status(p.getStatus() == null ? null : p.getStatus().name())
                .managerId(p.getManagerId())
                .build();
    }
}
