package com.iwas.user.dto;

import com.iwas.user.enums.UserRole;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(max = 100)
    private String fullName;

    @Size(max = 100)
    private String username;

    @Size(max = 20)
    private String phone;

    private Long departmentId;

    @Size(max = 100)
    private String position;

    /** Only ADMIN may change role. HR requests with this field set will be rejected. */
    private UserRole role;
}
