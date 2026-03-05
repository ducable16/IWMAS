package com.roamtrip.utils;

import com.roamtrip.entity.enums.ErrorCode;
import com.roamtrip.entity.user.User;
import com.roamtrip.exception.EntityNotFoundException;
import com.roamtrip.repository.UserRepository;
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
