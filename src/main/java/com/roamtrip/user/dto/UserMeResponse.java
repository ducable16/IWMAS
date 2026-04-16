package com.roamtrip.user.dto;

import com.roamtrip.user.enums.UserRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserMeResponse {
    private Long id;
    private String email;
    private String username;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private Long departmentId;
    private String position;
    private UserRole role;
    private Boolean verified;
    private Boolean active;
}
