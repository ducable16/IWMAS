package com.roamtrip.auth.dto;

import com.roamtrip.user.dto.UserMeResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private UserMeResponse user;
}
