package com.iwas.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
