package com.iwas.search.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.search")
public class SearchProperties {

    private final Autocomplete autocomplete = new Autocomplete();
    private final Elasticsearch elasticsearch = new Elasticsearch();

    @Data
    public static class Autocomplete {
        private int maxSuggestions = 10;
        private int cacheTtlSeconds = 60;
        private int minPrefixLength = 1;
    }

    @Data
    public static class Elasticsearch {
        private String userIndex = "iwas-users";
        private String projectIndex = "iwas-projects";
        private int maxPageSize = 50;
    }
}
