package com.iwas.notification.service;

import com.iwas.notification.enums.NotificationType;
import com.iwas.task.entity.Task;
import com.iwas.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final TaskRepository taskRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void notifyOverdueTasks() {
        LocalDate today = LocalDate.now();
        List<Task> overdueTasks = taskRepository.findOverdueTasksNotNotifiedToday(today);
        for (Task task : overdueTasks) {
            notificationService.send(
                    task.getAssigneeId(), NotificationType.TASK_OVERDUE,
                    "Task của bạn đã quá hạn",
                    "Task \"" + task.getTitle() + "\" đã quá hạn kể từ " + task.getDueDate() + ".",
                    "TASK", task.getId());
            task.setLastOverdueNotifiedAt(today);
            taskRepository.save(task);
        }
        log.info("[Scheduler] Notified {} overdue task(s)", overdueTasks.size());
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void notifyDeadlineReminders() {
        LocalDate today = LocalDate.now();
        List<LocalDate> targetDates = List.of(today.plusDays(1), today.plusDays(3));
        List<Task> tasks = taskRepository.findTasksDueOn(targetDates);
        for (Task task : tasks) {
            long daysLeft = ChronoUnit.DAYS.between(today, task.getDueDate());
            notificationService.send(
                    task.getAssigneeId(), NotificationType.DEADLINE_REMINDER,
                    "Task sắp đến hạn",
                    "Task \"" + task.getTitle() + "\" sẽ đến hạn sau " + daysLeft + " ngày.",
                    "TASK", task.getId());
        }
        log.info("[Scheduler] Sent deadline reminders for {} task(s)", tasks.size());
    }
}
