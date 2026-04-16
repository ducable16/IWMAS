package com.roamtrip.user.dto;

import com.roamtrip.user.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank
    @Size(max = 100)
    private String fullName;

    @Size(max = 100)
    private String username;

    @Size(max = 20)
    private String phone;

    private Long departmentId;

    @Size(max = 100)
    private String position;

    private UserRole role = UserRole.TEAM_MEMBER;
}
