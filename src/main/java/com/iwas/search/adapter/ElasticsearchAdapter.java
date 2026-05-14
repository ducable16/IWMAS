package com.iwas.search.adapter;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.iwas.search.config.SearchProperties;
import com.iwas.search.dto.ProjectSearchResult;
import com.iwas.search.dto.SearchRequest;
import com.iwas.search.dto.SearchResponse;
import com.iwas.search.dto.SuggestionItem;
import com.iwas.search.dto.UserSearchResult;
import com.iwas.search.entity.ProjectSearchDocument;
import com.iwas.search.entity.UserSearchDocument;
import com.iwas.search.repository.ElasticsearchProjectRepository;
import com.iwas.search.repository.ElasticsearchUserRepository;
import com.iwas.search.service.ElasticsearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchAdapter implements ElasticsearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final ElasticsearchUserRepository userSearchRepository;
    private final ElasticsearchProjectRepository projectSearchRepository;
    private final SearchProperties properties;

    // -------------------------------------------------------------------------
    // User
    // -------------------------------------------------------------------------

    @Override
    public SearchResponse<UserSearchResult> searchUsers(SearchRequest request) {
        long start = System.currentTimeMillis();
        int size = Math.min(request.getSize(), properties.getElasticsearch().getMaxPageSize());

        Query query = Query.of(q -> q.bool(b -> b
                .should(s -> s.multiMatch(m -> m
                        .query(request.getQuery())
                        .fields("fullName", "position", "email")
                        .fuzziness("AUTO")))
                .should(s -> s.matchPhrasePrefix(p -> p
                        .field("fullName")
                        .query(request.getQuery())))
                .filter(f -> f.term(t -> t.field("isActive").value(true)))));

        NativeQuery nq = NativeQuery.builder()
                .withQuery(query)
                .withPageable(PageRequest.of(request.getPage(), size))
                .build();

        if (request.getSortBy() != null && !request.getSortBy().isBlank()) {
            nq.setSort(org.springframework.data.domain.Sort.by(
                    "asc".equalsIgnoreCase(request.getSortDir())
                            ? org.springframework.data.domain.Sort.Direction.ASC
                            : org.springframework.data.domain.Sort.Direction.DESC,
                    request.getSortBy()));
        }

        SearchHits<UserSearchDocument> hits = elasticsearchOperations.search(nq, UserSearchDocument.class);

        List<UserSearchResult> items = hits.getSearchHits().stream()
                .map(h -> toUserResult(h.getContent()))
                .collect(Collectors.toList());

        return SearchResponse.<UserSearchResult>builder()
                .items(items)
                .total(hits.getTotalHits())
                .page(request.getPage())
                .size(size)
                .source("elasticsearch")
                .tookMs(System.currentTimeMillis() - start)
                .build();
    }

    @Override
    public List<SuggestionItem> autocompleteUsers(String prefix, int topN) {
        Query query = Query.of(q -> q.bool(b -> b
                .should(s -> s.matchPhrasePrefix(p -> p.field("fullName").query(prefix)))
                .should(s -> s.matchPhrasePrefix(p -> p.field("position").query(prefix)))
                .filter(f -> f.term(t -> t.field("isActive").value(true)))));

        NativeQuery nq = NativeQuery.builder()
                .withQuery(query)
                .withPageable(PageRequest.of(0, topN))
                .build();

        SearchHits<UserSearchDocument> hits = elasticsearchOperations.search(nq, UserSearchDocument.class);

        return hits.getSearchHits().stream()
                .map(h -> SuggestionItem.builder()
                        .term(h.getContent().getFullName())
                        .entityId(h.getContent().getId())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<SuggestionItem> autocompleteUsersExcluding(String prefix, int topN, Set<Long> excludeIds) {
        Query query = Query.of(q -> q.bool(b -> {
            b.should(s -> s.matchPhrasePrefix(p -> p.field("fullName").query(prefix)))
             .should(s -> s.matchPhrasePrefix(p -> p.field("position").query(prefix)))
             .filter(f -> f.term(t -> t.field("isActive").value(true)));
            if (!excludeIds.isEmpty()) {
                List<String> idStrings = excludeIds.stream().map(String::valueOf).toList();
                b.mustNot(mn -> mn.ids(i -> i.values(idStrings)));
            }
            return b;
        }));

        NativeQuery nq = NativeQuery.builder()
                .withQuery(query)
                .withPageable(PageRequest.of(0, topN))
                .build();

        SearchHits<UserSearchDocument> hits = elasticsearchOperations.search(nq, UserSearchDocument.class);

        return hits.getSearchHits().stream()
                .map(h -> SuggestionItem.builder()
                        .term(h.getContent().getFullName())
                        .entityId(h.getContent().getId())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public void indexUser(UserSearchResult user) {
        UserSearchDocument doc = UserSearchDocument.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .position(user.getPosition())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .isActive(true)
                .build();
        userSearchRepository.save(doc);
    }

    @Override
    public void deleteUser(Long userId) {
        userSearchRepository.deleteById(userId);
    }

    // -------------------------------------------------------------------------
    // Project
    // -------------------------------------------------------------------------

    @Override
    public SearchResponse<ProjectSearchResult> searchProjects(SearchRequest request) {
        long start = System.currentTimeMillis();
        int size = Math.min(request.getSize(), properties.getElasticsearch().getMaxPageSize());

        Query query = Query.of(q -> q.bool(b -> b
                .should(s -> s.multiMatch(m -> m
                        .query(request.getQuery())
                        .fields("name")
                        .fuzziness("AUTO")))
                .should(s -> s.matchPhrasePrefix(p -> p
                        .field("name")
                        .query(request.getQuery())))
                .should(s -> s.prefix(p -> p
                        .field("code")
                        .value(request.getQuery().toLowerCase())))
                .minimumShouldMatch("1")
                .filter(f -> f.term(t -> t.field("isDeleted").value(false)))));

        NativeQuery nq = NativeQuery.builder()
                .withQuery(query)
                .withPageable(PageRequest.of(request.getPage(), size))
                .build();

        if (request.getSortBy() != null && !request.getSortBy().isBlank()) {
            nq.setSort(org.springframework.data.domain.Sort.by(
                    "asc".equalsIgnoreCase(request.getSortDir())
                            ? org.springframework.data.domain.Sort.Direction.ASC
                            : org.springframework.data.domain.Sort.Direction.DESC,
                    request.getSortBy()));
        }

        SearchHits<ProjectSearchDocument> hits = elasticsearchOperations.search(nq, ProjectSearchDocument.class);

        List<ProjectSearchResult> items = hits.getSearchHits().stream()
                .map(h -> toProjectResult(h.getContent()))
                .collect(Collectors.toList());

        return SearchResponse.<ProjectSearchResult>builder()
                .items(items)
                .total(hits.getTotalHits())
                .page(request.getPage())
                .size(size)
                .source("elasticsearch")
                .tookMs(System.currentTimeMillis() - start)
                .build();
    }

    @Override
    public List<SuggestionItem> autocompleteProjects(String prefix, int topN) {
        Query query = Query.of(q -> q.bool(b -> b
                .should(s -> s.matchPhrasePrefix(p -> p.field("name").query(prefix)))
                .filter(f -> f.term(t -> t.field("isDeleted").value(false)))));

        NativeQuery nq = NativeQuery.builder()
                .withQuery(query)
                .withPageable(PageRequest.of(0, topN))
                .build();

        SearchHits<ProjectSearchDocument> hits = elasticsearchOperations.search(nq, ProjectSearchDocument.class);

        return hits.getSearchHits().stream()
                .map(h -> SuggestionItem.builder()
                        .term(h.getContent().getName())
                        .entityId(h.getContent().getId())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public void indexProject(ProjectSearchResult project) {
        ProjectSearchDocument doc = ProjectSearchDocument.builder()
                .id(project.getId())
                .name(project.getName())
                .code(project.getCode())
                .status(project.getStatus())
                .managerId(project.getManagerId())
                .isDeleted(false)
                .build();
        projectSearchRepository.save(doc);
    }

    @Override
    public void deleteProject(Long projectId) {
        projectSearchRepository.deleteById(projectId);
    }

    // -------------------------------------------------------------------------
    // Health
    // -------------------------------------------------------------------------

    @Override
    public boolean isHealthy() {
        try {
            elasticsearchOperations.indexOps(UserSearchDocument.class).exists();
            return true;
        } catch (Exception e) {
            log.warn("Elasticsearch health check failed: {}", e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private UserSearchResult toUserResult(UserSearchDocument doc) {
        return UserSearchResult.builder()
                .id(doc.getId())
                .email(doc.getEmail())
                .fullName(doc.getFullName())
                .position(doc.getPosition())
                .avatarUrl(doc.getAvatarUrl())
                .role(doc.getRole())
                .build();
    }

    private ProjectSearchResult toProjectResult(ProjectSearchDocument doc) {
        return ProjectSearchResult.builder()
                .id(doc.getId())
                .name(doc.getName())
                .code(doc.getCode())
                .status(doc.getStatus())
                .managerId(doc.getManagerId())
                .build();
    }
}
