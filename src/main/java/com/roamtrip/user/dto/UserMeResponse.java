package com.roamtrip.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserMeResponse {
    private Long id;
    private String email;
    private String name;
    private Boolean active;
}
