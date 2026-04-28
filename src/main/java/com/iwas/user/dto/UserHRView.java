package com.iwas.user.dto;

import com.iwas.user.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserHRView {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private String position;
    private UserRole role;
    private Boolean verified;
    private Boolean active;
    private LocalDateTime createdAt;
}