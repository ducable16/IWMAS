package com.iwas.search.service;

import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import com.iwas.project.service.ProjectService;
import com.iwas.search.config.SearchProperties;
import com.iwas.search.dto.AutocompleteResponse;
import com.iwas.search.dto.ProjectSearchResult;
import com.iwas.search.dto.SearchRequest;
import com.iwas.search.dto.SearchResponse;
import com.iwas.search.dto.SuggestionItem;
import com.iwas.search.dto.UserSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private static final String ENTITY_USER = "user";
    private static final String ENTITY_PROJECT = "project";

    private final RedisService cache;
    private final ElasticsearchService engine;
    private final SearchFallbackService fallback;
    private final SearchProperties properties;
    private final ProjectService projectService;

    // -------------------------------------------------------------------------
    // User
    // -------------------------------------------------------------------------

    public AutocompleteResponse autocomplete(String query, Long projectId, Long excludeProjectId) {
        long start = System.currentTimeMillis();
        String prefix = normalize(query);
        int minLen = properties.getAutocomplete().getMinPrefixLength();
        if (prefix.length() < minLen) {
            throw new AppException(ErrorCode.SEARCH_QUERY_TOO_SHORT,
                    "Query must be at least " + minLen + " characters");
        }
        int topN = properties.getAutocomplete().getMaxSuggestions();

        if (projectId != null) {
            List<SuggestionItem> suggestions = projectService
                    .searchProjectMembers(projectId, query, List.of(), topN)
                    .stream()
                    .map(u -> SuggestionItem.builder().term(u.getFullName()).entityId(u.getId()).build())
                    .toList();
            return AutocompleteResponse.builder()
                    .prefix(prefix).suggestions(suggestions).source("database")
                    .tookMs(System.currentTimeMillis() - start).build();
        }

        if (excludeProjectId != null) {
            Set<Long> excludeIds = projectService.getExistingParticipantIds(excludeProjectId);
            List<SuggestionItem> suggestions = List.of();
            String source = "elasticsearch";
            try {
                suggestions = engine.autocompleteUsersExcluding(prefix, topN, excludeIds);
            } catch (Exception e) {
                log.warn("Elasticsearch autocomplete (exclude) failed, falling back to DB: {}", e.getMessage());
            }
            if (suggestions.isEmpty()) {
                suggestions = fallback.autocompleteUsersExcluding(prefix, topN, excludeIds);
                source = "database";
            }
            return AutocompleteResponse.builder()
                    .prefix(prefix).suggestions(suggestions).source(source)
                    .tookMs(System.currentTimeMillis() - start).build();
        }

        List<SuggestionItem> cached = cache.getSuggestions(ENTITY_USER, prefix);
        if (!cached.isEmpty()) {
            return AutocompleteResponse.builder()
                    .prefix(prefix).suggestions(cached).source("redis")
                    .tookMs(System.currentTimeMillis() - start).build();
        }

        List<SuggestionItem> suggestions = List.of();
        String source = "elasticsearch";
        try {
            suggestions = engine.autocompleteUsers(prefix, topN);
        } catch (Exception e) {
            log.warn("Elasticsearch autocomplete failed, falling back to DB: {}", e.getMessage());
        }

        if (suggestions.isEmpty()) {
            suggestions = fallback.autocompleteUsers(prefix, topN);
            source = "database";
        }

        warmCacheAsync(ENTITY_USER, prefix, suggestions);

        return AutocompleteResponse.builder()
                .prefix(prefix).suggestions(suggestions).source(source)
                .tookMs(System.currentTimeMillis() - start).build();
    }

    public SearchResponse<UserSearchResult> search(SearchRequest request) {
        boolean hasQuery = request.getQuery() != null && !request.getQuery().isBlank();
        boolean hasSkillFilter = request.getRequiredSkills() != null && !request.getRequiredSkills().isEmpty();
        if (!hasQuery && !hasSkillFilter) {
            throw new AppException(ErrorCode.SEARCH_QUERY_TOO_SHORT,
                    "Provide a search query or at least one required skill");
        }
        try {
            SearchResponse<UserSearchResult> result = engine.searchUsers(request);
            if (result.getItems() != null && !result.getItems().isEmpty()) {
                return result;
            }
        } catch (Exception e) {
            log.warn("Elasticsearch search failed, falling back to DB: {}", e.getMessage());
        }
        return fallback.searchUsers(request);
    }

    // -------------------------------------------------------------------------
    // Project
    // -------------------------------------------------------------------------

    public AutocompleteResponse autocompleteProjects(String query) {
        long start = System.currentTimeMillis();
        String prefix = normalize(query);
        int minLen = properties.getAutocomplete().getMinPrefixLength();
        if (prefix.length() < minLen) {
            throw new AppException(ErrorCode.SEARCH_QUERY_TOO_SHORT,
                    "Query must be at least " + minLen + " characters");
        }
        int topN = properties.getAutocomplete().getMaxSuggestions();

        List<SuggestionItem> cached = cache.getSuggestions(ENTITY_PROJECT, prefix);
        if (!cached.isEmpty()) {
            return AutocompleteResponse.builder()
                    .prefix(prefix).suggestions(cached).source("redis")
                    .tookMs(System.currentTimeMillis() - start).build();
        }

        List<SuggestionItem> suggestions = List.of();
        String source = "elasticsearch";
        try {
            suggestions = engine.autocompleteProjects(prefix, topN);
        } catch (Exception e) {
            log.warn("Elasticsearch project autocomplete failed, falling back to DB: {}", e.getMessage());
        }

        if (suggestions.isEmpty()) {
            suggestions = fallback.autocompleteProjects(prefix, topN);
            source = "database";
        }

        warmCacheAsync(ENTITY_PROJECT, prefix, suggestions);

        return AutocompleteResponse.builder()
                .prefix(prefix).suggestions(suggestions).source(source)
                .tookMs(System.currentTimeMillis() - start).build();
    }

    public SearchResponse<ProjectSearchResult> searchProjects(SearchRequest request) {
        try {
            SearchResponse<ProjectSearchResult> result = engine.searchProjects(request);
            if (result.getItems() != null && !result.getItems().isEmpty()) {
                return result;
            }
        } catch (Exception e) {
            log.warn("Elasticsearch project search failed, falling back to DB: {}", e.getMessage());
        }
        return fallback.searchProjects(request);
    }

    // -------------------------------------------------------------------------
    // Shared
    // -------------------------------------------------------------------------

    @Async
    public void warmCacheAsync(String entity, String prefix, List<SuggestionItem> items) {
        try {
            cache.putSuggestions(entity, prefix, items);
        } catch (Exception e) {
            log.warn("Cache warm-up failed for entity={} prefix={}: {}", entity, prefix, e.getMessage());
        }
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
}
