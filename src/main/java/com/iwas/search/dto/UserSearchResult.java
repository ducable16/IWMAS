package com.iwas.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResult {
    private Long id;
    private String email;
    private String fullName;
    private String position;
    private String avatarUrl;
    private String role;
}
