package com.iwas.user.mapper;

import com.iwas.user.dto.UserMeResponse;
import com.iwas.user.entity.User;

public final class UserMapper {

    private UserMapper() {}

    public static UserMeResponse toUserMeResponse(User user) {
        if (user == null) return null;
        return UserMeResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .position(user.getPosition())
                .role(user.getRole())
                .verified(user.getIsVerified())
                .active(user.getIsActive())
                .build();
    }
}
