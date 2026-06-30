package com.iwas.task.service;

import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import com.iwas.notification.NotificationMessages;
import com.iwas.notification.enums.NotificationType;
import com.iwas.notification.service.NotificationService;
import com.iwas.project.service.ProjectService;
import com.iwas.task.dto.TaskCommentRequest;
import com.iwas.task.dto.TaskCommentResponse;
import com.iwas.task.entity.Task;
import com.iwas.task.entity.TaskComment;
import com.iwas.task.repository.TaskCommentRepository;
import com.iwas.task.repository.TaskRepository;
import com.iwas.user.dto.UserPublicView;
import com.iwas.user.entity.User;
import com.iwas.user.mapper.UserMapper;
import com.iwas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskCommentService {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@\\[uid:(\\d+)]");

    private final TaskCommentRepository taskCommentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ProjectService projectService;
    private final UserMapper userMapper;

    public List<TaskCommentResponse> getCommentsByTask(Long taskId) {
        Task task = findTask(taskId);
        List<TaskComment> comments = taskCommentRepository
                .findByTaskIdAndIsDeletedFalseOrderByCreatedAtAsc(taskId);

        if (comments.isEmpty()) return List.of();

        Map<Long, User> authorMap = userRepository.findAllById(
                comments.stream().map(TaskComment::getAuthorId).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getId, u -> u));

        Set<Long> allMentionedIds = comments.stream()
                .flatMap(c -> parseMentions(c.getContent()).stream())
                .collect(Collectors.toSet());
        Map<Long, User> mentionedUserMap = resolveValidMentions(allMentionedIds, task.getProjectId());

        return comments.stream()
                .map(c -> {
                    Map<Long, User> perComment = parseMentions(c.getContent()).stream()
                            .filter(mentionedUserMap::containsKey)
                            .collect(Collectors.toMap(id -> id, mentionedUserMap::get));
                    return toResponse(c, authorMap.get(c.getAuthorId()), perComment);
                })
                .toList();
    }

    @Transactional
    public TaskCommentResponse addComment(Long taskId, TaskCommentRequest request, Long authorId) {
        Task task = findTask(taskId);

        TaskComment comment = new TaskComment();
        comment.setTaskId(taskId);
        comment.setAuthorId(authorId);
        comment.setContent(request.getContent().trim());
        comment = taskCommentRepository.save(comment);

        Set<Long> mentionedIds = parseMentions(comment.getContent());
        Map<Long, User> validMentions = resolveValidMentions(mentionedIds, task.getProjectId());
        sendMentionNotifications(validMentions, task, authorId, Set.of());

        User author = userRepository.findById(authorId).orElse(null);
        return toResponse(comment, author, validMentions);
    }

    @Transactional
    public TaskCommentResponse updateComment(Long commentId, TaskCommentRequest request, Long currentUserId) {
        TaskComment comment = findComment(commentId);
        if (!comment.getAuthorId().equals(currentUserId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        Set<Long> oldMentionedIds = parseMentions(comment.getContent());

        comment.setContent(request.getContent().trim());
        comment = taskCommentRepository.save(comment);

        Task task = findTask(comment.getTaskId());
        Set<Long> newMentionedIds = parseMentions(comment.getContent());
        Map<Long, User> validMentions = resolveValidMentions(newMentionedIds, task.getProjectId());
        sendMentionNotifications(validMentions, task, currentUserId, oldMentionedIds);

        User author = userRepository.findById(currentUserId).orElse(null);
        return toResponse(comment, author, validMentions);
    }

    @Transactional
    public void deleteComment(Long commentId, Long currentUserId) {
        TaskComment comment = findComment(commentId);
        if (!comment.getAuthorId().equals(currentUserId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        comment.setIsDeleted(true);
        taskCommentRepository.save(comment);
    }

    private Set<Long> parseMentions(String content) {
        if (content == null || !content.contains("@[uid:")) return Set.of();
        Set<Long> result = new LinkedHashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            result.add(Long.parseLong(matcher.group(1)));
        }
        return result;
    }

    private Map<Long, User> resolveValidMentions(Set<Long> mentionedIds, Long projectId) {
        if (mentionedIds.isEmpty()) return Map.of();
        return userRepository.findAllById(mentionedIds).stream()
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .filter(u -> projectService.isProjectParticipant(projectId, u.getId()))
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    private void sendMentionNotifications(Map<Long, User> validMentions, Task task,
                                          Long authorId, Set<Long> alreadyNotified) {
        validMentions.entrySet().stream()
                .filter(e -> !e.getKey().equals(authorId))
                .filter(e -> !alreadyNotified.contains(e.getKey()))
                .forEach(e -> notificationService.send(
                        e.getKey(), NotificationType.COMMENT_MENTION,
                        NotificationMessages.commentMention(task.getTitle()),
                        "TASK", task.getId()));
    }

    private Task findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));
    }

    private TaskComment findComment(Long id) {
        return taskCommentRepository.findById(id)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.TASK_COMMENT_NOT_FOUND));
    }

    private TaskCommentResponse toResponse(TaskComment c, User author, Map<Long, User> resolvedMentions) {
        Map<Long, UserPublicView> mentions = resolvedMentions.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> toPublicView(e.getValue())));

        return TaskCommentResponse.builder()
                .id(c.getId())
                .taskId(c.getTaskId())
                .author(toPublicView(author))
                .content(c.getContent())
                .mentions(mentions)
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private UserPublicView toPublicView(User user) {
        return userMapper.toPublicView(user);
    }
}
