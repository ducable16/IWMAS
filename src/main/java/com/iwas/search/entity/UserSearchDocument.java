package com.iwas.search.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "iwas-users")
@Setting(settingPath = "/elasticsearch/iwas-users-settings.json")
public class UserSearchDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Keyword)
    private String email;

    @Field(type = FieldType.Text, analyzer = "edge_ngram_analyzer", searchAnalyzer = "standard")
    private String fullName;

    @Field(type = FieldType.Text, analyzer = "edge_ngram_analyzer", searchAnalyzer = "standard")
    private String position;

    @Field(type = FieldType.Keyword)
    private String role;

    @Field(type = FieldType.Keyword)
    private String avatarUrl;

    @Field(type = FieldType.Boolean)
    private Boolean isActive;
}
