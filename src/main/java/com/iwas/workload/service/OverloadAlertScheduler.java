package com.iwas.workload.service;

import com.iwas.user.entity.User;
import com.iwas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Daily overload check for every active user. Computes each member's workload
 * live from the v3 simulation and sends an OVERLOAD_WARNING when their badge is
 * OVERDUE or WILL_SLIP — nothing is persisted. Each evaluateOverload() call is
 * independently transactional, so one user's failure does not affect the others.
 */
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
