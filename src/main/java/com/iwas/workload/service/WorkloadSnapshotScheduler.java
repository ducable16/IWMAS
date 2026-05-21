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
public class WorkloadSnapshotScheduler {

    private final UserRepository userRepository;
    private final WorkloadService workloadService;

    /**
     * End-of-day workload snapshot for every active user. Each takeSnapshot()
     * call is independently transactional, so one user's failure does not roll
     * back the others.
     */
    @Scheduled(cron = "0 5 4 * * *")
    public void takeDailySnapshots() {
        List<User> users = userRepository.findAllActiveUsers();
        int ok = 0;
        for (User user : users) {
            try {
                workloadService.takeSnapshot(user.getId());
                ok++;
            } catch (Exception e) {
                log.warn("[Scheduler] Workload snapshot failed for user {}: {}",
                        user.getId(), e.getMessage());
            }
        }
        log.info("[Scheduler] Took {} workload snapshot(s) out of {} active user(s)",
                ok, users.size());
    }
}
