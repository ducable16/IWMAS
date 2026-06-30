package com.iwas.user.dto;

import com.iwas.user.enums.UserRole;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(max = 100)
    private String fullName;

    @Size(max = 20)
    private String phone;

    @Size(max = 100)
    private String position;

    private UserRole role;
}
