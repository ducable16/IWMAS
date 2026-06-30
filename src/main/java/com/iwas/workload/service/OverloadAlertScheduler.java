package com.iwas.workload.service;

import com.iwas.user.entity.User;
import com.iwas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OverloadAlertScheduler {

    private final UserRepository userRepository;
    private final WorkloadService workloadService;

    @Scheduled(cron = "0 5 4 * * *")
    public void evaluateDailyOverload() {
        List<User> users = userRepository.findAllActiveUsers();
        int ok = 0;
        for (User user : users) {
            try {
                workloadService.evaluateOverload(user.getId());
                ok++;
            } catch (Exception e) {
                log.warn("[Scheduler] Overload check failed for user {}: {}",
                        user.getId(), e.getMessage());
            }
        }
        log.info("[Scheduler] Ran overload check for {} of {} active user(s)", ok, users.size());
    }
}
