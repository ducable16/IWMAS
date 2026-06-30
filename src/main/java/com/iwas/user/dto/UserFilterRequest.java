package com.iwas.user.dto;

import com.iwas.user.enums.UserRole;
import lombok.Data;

@Data
public class UserFilterRequest {

    private String search;
    private UserRole role;
    private String position;
    private Boolean active;
    private Boolean verified;

    private String sortBy = "fullName";
    private String sortDirection = "ASC";
    private int page = 0;
    private int size = 20;
}