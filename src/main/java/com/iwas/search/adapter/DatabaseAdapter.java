package com.iwas.search.adapter;

import com.iwas.search.config.SearchProperties;
import com.iwas.search.dto.SearchRequest;
import com.iwas.search.dto.SearchResponse;
import com.iwas.search.dto.SuggestionItem;
import com.iwas.search.dto.UserSearchResult;
import com.iwas.search.service.SearchFallbackService;
import com.iwas.user.entity.User;
import com.iwas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseAdapter implements SearchFallbackService {

    private final UserRepository userRepository;
    private final SearchProperties properties;

    @Override
    public SearchResponse<UserSearchResult> searchUsers(SearchRequest request) {
        long start = System.currentTimeMillis();
        int size = Math.min(request.getSize(), properties.getElasticsearch().getMaxPageSize());
        String like = "%" + request.getQuery().toLowerCase() + "%";

        Specification<User> spec = (root, q, cb) -> cb.and(
                cb.equal(root.get("isDeleted"), false),
                cb.or(
                        cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(cb.lower(root.get("position")), like),
                        cb.like(cb.lower(root.get("email")), like)));

        PageRequest pageable = PageRequest.of(request.getPage(), size,
                Sort.by(Sort.Direction.ASC, "fullName"));

        Page<User> page = userRepository.findAll(spec, pageable);

        List<UserSearchResult> items = page.getContent().stream()
                .map(this::toResult)
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
    public List<SuggestionItem> autocompleteUsers(String prefix, int topN) {
        SearchRequest req = SearchRequest.builder()
                .query(prefix)
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

    private UserSearchResult toResult(User u) {
        return UserSearchResult.builder()
                .id(u.getId())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .position(u.getPosition())
                .avatarUrl(u.getAvatarUrl())
                .role(u.getRole() == null ? null : u.getRole().name())
                .build();
    }
}
