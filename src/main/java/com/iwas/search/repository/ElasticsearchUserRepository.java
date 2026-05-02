package com.iwas.search.repository;

import com.iwas.search.entity.UserSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ElasticsearchUserRepository extends ElasticsearchRepository<UserSearchDocument, Long> {
}
