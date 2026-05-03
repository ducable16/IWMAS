package com.iwas.search.repository;

import com.iwas.search.entity.ProjectSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ElasticsearchProjectRepository extends ElasticsearchRepository<ProjectSearchDocument, Long> {
}
