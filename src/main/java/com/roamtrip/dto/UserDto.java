package com.roamtrip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class UserDto {
    private Long userId;
    private String email;
    private String fullName;
    private String username;
}
