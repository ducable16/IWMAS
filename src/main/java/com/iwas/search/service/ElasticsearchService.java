package com.iwas.search.service;

import com.iwas.search.dto.ProjectSearchResult;
import com.iwas.search.dto.SearchRequest;
import com.iwas.search.dto.SearchResponse;
import com.iwas.search.dto.SuggestionItem;
import com.iwas.search.dto.UserIndexCommand;
import com.iwas.search.dto.UserSearchResult;
import com.iwas.user.enums.UserRole;

import java.util.List;
import java.util.Set;

public interface ElasticsearchService {

    // --- User ---
    SearchResponse<UserSearchResult> searchUsers(SearchRequest request);
    List<SuggestionItem> autocompleteUsers(String prefix, int topN, UserRole role);
    List<SuggestionItem> autocompleteUsersExcluding(String prefix, int topN, Set<Long> excludeIds, UserRole role);
    void indexUser(UserIndexCommand command);
    void deleteUser(Long userId);

    // --- Project ---
    SearchResponse<ProjectSearchResult> searchProjects(SearchRequest request);
    List<SuggestionItem> autocompleteProjects(String prefix, int topN);
    void indexProject(ProjectSearchResult project);
    void deleteProject(Long projectId);

    boolean isHealthy();
}
