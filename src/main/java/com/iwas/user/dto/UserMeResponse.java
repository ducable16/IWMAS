package com.iwas.user.dto;

import com.iwas.user.enums.UserRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserMeResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private String position;
    private UserRole role;
    private Boolean verified;
    private Boolean active;
}
