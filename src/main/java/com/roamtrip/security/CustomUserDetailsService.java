package com.roamtrip.security;

import com.roamtrip.common.enums.ErrorCode;
import com.roamtrip.user.entity.User;
import com.roamtrip.common.exception.EntityNotFoundException;
import com.roamtrip.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        return CustomUserDetails.builder()
                .user(user)
                .build();
    }
}
