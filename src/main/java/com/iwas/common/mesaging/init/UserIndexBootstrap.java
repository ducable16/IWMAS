package com.iwas.common.mesaging.init;

import com.iwas.search.entity.UserSearchDocument;
import com.iwas.search.repository.ElasticsearchUserRepository;
import com.iwas.user.entity.User;
import com.iwas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.search", name = "bootstrap-on-startup", havingValue = "true", matchIfMissing = false)
public class UserIndexBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final ElasticsearchUserRepository userSearchRepository;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<User> users = userRepository.findAllActiveUsers();
            List<UserSearchDocument> docs = users.stream().map(this::toDoc).toList();
            userSearchRepository.saveAll(docs);
            log.info("Search bootstrap: indexed {} users into Elasticsearch", docs.size());
        } catch (Exception e) {
            log.error("Search bootstrap failed: {}", e.getMessage(), e);
        }
    }

    private UserSearchDocument toDoc(User u) {
        return UserSearchDocument.builder()
                .id(u.getId())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .position(u.getPosition())
                .avatarUrl(u.getAvatarUrl())
                .role(u.getRole() == null ? null : u.getRole().name())
                .isActive(Boolean.TRUE.equals(u.getIsActive()))
                .build();
    }
}
