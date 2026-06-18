package com.iwas.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Input cho việc index user vào search engine (write path).
 * Tách khỏi {@link UserSearchResult} (read path) để tránh nhầm lẫn:
 * ở đây {@code avatarId} là KEY lưu trong storage, KHÔNG phải URL đã resolve.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserIndexCommand {
    private Long id;
    private String email;
    private String fullName;
    private String position;
    private String avatarId;
    private String role;
}
