package com.iwas.search.service;

import com.iwas.search.dto.ProjectSearchResult;
import com.iwas.search.dto.SearchRequest;
import com.iwas.search.dto.SearchResponse;
import com.iwas.search.dto.SuggestionItem;
import com.iwas.search.dto.UserSearchResult;

import java.util.List;

public interface ElasticsearchService {

    // --- User ---
    SearchResponse<UserSearchResult> searchUsers(SearchRequest request);
    List<SuggestionItem> autocompleteUsers(String prefix, int topN);
    void indexUser(UserSearchResult user);
    void deleteUser(Long userId);

    // --- Project ---
    SearchResponse<ProjectSearchResult> searchProjects(SearchRequest request);
    List<SuggestionItem> autocompleteProjects(String prefix, int topN);
    void indexProject(ProjectSearchResult project);
    void deleteProject(Long projectId);

    boolean isHealthy();
}
