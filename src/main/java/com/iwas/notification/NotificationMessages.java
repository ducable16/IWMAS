package com.iwas.notification;

import java.time.LocalDate;

public final class NotificationMessages {

    private NotificationMessages() {}

    public record NotificationContent(String title, String content) {}

    public static NotificationContent newTaskAssigned(String taskTitle) {
        return new NotificationContent(
                "You have been assigned a new task",
                "Task \"" + taskTitle + "\" has been assigned to you."
        );
    }

    public static NotificationContent taskReassigned(String taskTitle) {
        return new NotificationContent(
                "You have been assigned a task",
                "Task \"" + taskTitle + "\" has been assigned to you."
        );
    }

    public static NotificationContent taskOverdue(String taskTitle, LocalDate dueDate) {
        return new NotificationContent(
                "Your task is overdue",
                "Task \"" + taskTitle + "\" has been overdue since " + dueDate + "."
        );
    }

    public static NotificationContent deadlineReminder(String taskTitle, long daysLeft) {
        return new NotificationContent(
                "Task deadline approaching",
                "Task \"" + taskTitle + "\" is due in " + daysLeft + " day(s)."
        );
    }

    public static NotificationContent taskStatusChanged(String taskTitle, String statusName) {
        return new NotificationContent(
                "Task status changed",
                "Task \"" + taskTitle + "\" has moved to " + statusName + "."
        );
    }

    public static NotificationContent projectAdded(String projectName) {
        return new NotificationContent(
                "You have been added to a project",
                "You have been added to project \"" + projectName + "\"."
        );
    }

    public static NotificationContent commentMention(String taskTitle) {
        return new NotificationContent(
                "You were mentioned in a comment",
                "You were @mentioned in task \"" + taskTitle + "\"."
        );
    }

    public static NotificationContent overloadWarning(String utilizationPercent) {
        return new NotificationContent(
                "Workload overload warning",
                "This week's workload has reached " + utilizationPercent
                        + "% of capacity. Please review and adjust your workload."
        );
    }
}
