package com.iwas.search.service;

import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import com.iwas.search.config.SearchProperties;
import com.iwas.search.dto.AutocompleteResponse;
import com.iwas.search.dto.SearchRequest;
import com.iwas.search.dto.SearchResponse;
import com.iwas.search.dto.SuggestionItem;
import com.iwas.search.dto.UserSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private static final String ENTITY_USER = "user";

    private final RedisService cache;
    private final ElasticsearchService engine;
    private final SearchFallbackService fallback;
    private final SearchProperties properties;

    public AutocompleteResponse autocomplete(String query) {
        long start = System.currentTimeMillis();
        String prefix = normalize(query);
        int minLen = properties.getAutocomplete().getMinPrefixLength();
        if (prefix.length() < minLen) {
            throw new AppException(ErrorCode.SEARCH_QUERY_TOO_SHORT,
                    "Query must be at least " + minLen + " characters");
        }
        int topN = properties.getAutocomplete().getMaxSuggestions();

        // 1. Redis
        List<SuggestionItem> cached = cache.getSuggestions(ENTITY_USER, prefix);
        if (!cached.isEmpty()) {
            return AutocompleteResponse.builder()
                    .prefix(prefix).suggestions(cached).source("redis")
                    .tookMs(System.currentTimeMillis() - start).build();
        }

        // 2. Elasticsearch
        List<SuggestionItem> suggestions = List.of();
        String source = "elasticsearch";
        try {
            suggestions = engine.autocompleteUsers(prefix, topN);
        } catch (Exception e) {
            log.warn("Elasticsearch autocomplete failed, falling back to DB: {}", e.getMessage());
        }

        // 3. Database fallback
        if (suggestions.isEmpty()) {
            suggestions = fallback.autocompleteUsers(prefix, topN);
            source = "database";
        }

        // Backfill Redis (fire-and-forget)
        warmCacheAsync(prefix, suggestions);

        return AutocompleteResponse.builder()
                .prefix(prefix).suggestions(suggestions).source(source)
                .tookMs(System.currentTimeMillis() - start).build();
    }

    public SearchResponse<UserSearchResult> search(SearchRequest request) {
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

    @Async
    public void warmCacheAsync(String prefix, List<SuggestionItem> items) {
        try {
            cache.putSuggestions(ENTITY_USER, prefix, items);
        } catch (Exception e) {
            log.warn("Cache warm-up failed for prefix={}: {}", prefix, e.getMessage());
        }
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
}
