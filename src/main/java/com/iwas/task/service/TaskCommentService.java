package com.iwas.task.service;

import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import com.iwas.notification.enums.NotificationType;
import com.iwas.notification.service.NotificationService;
import com.iwas.task.dto.TaskCommentRequest;
import com.iwas.task.dto.TaskCommentResponse;
import com.iwas.task.entity.Task;
import com.iwas.task.entity.TaskComment;
import com.iwas.task.repository.TaskCommentRepository;
import com.iwas.task.repository.TaskRepository;
import com.iwas.user.dto.UserMeResponse;
import com.iwas.user.entity.User;
import com.iwas.user.mapper.UserMapper;
import com.iwas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskCommentService {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\p{L}+(?:\\s\\p{L}+){0,3})");

    private final TaskCommentRepository taskCommentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public List<TaskCommentResponse> getCommentsByTask(Long taskId) {
        ensureTaskExists(taskId);
        List<TaskComment> comments = taskCommentRepository
                .findByTaskIdAndIsDeletedFalseOrderByCreatedAtAsc(taskId);

        if (comments.isEmpty()) return List.of();

        Map<Long, User> userMap = userRepository.findAllById(
                comments.stream().map(TaskComment::getAuthorId).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getId, u -> u));

        return comments.stream()
                .map(c -> toResponse(c, userMap.get(c.getAuthorId())))
                .toList();
    }

    @Transactional
    public TaskCommentResponse addComment(Long taskId, TaskCommentRequest request, Long authorId) {
        Task task = taskRepository.findById(taskId)
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));

        TaskComment comment = new TaskComment();
        comment.setTaskId(taskId);
        comment.setAuthorId(authorId);
        comment.setContent(request.getContent().trim());
        comment = taskCommentRepository.save(comment);

        sendMentionNotifications(comment.getContent(), task, authorId);

        User author = userRepository.findById(authorId).orElse(null);
        return toResponse(comment, author);
    }

    @Transactional
    public TaskCommentResponse updateComment(Long commentId, TaskCommentRequest request, Long currentUserId) {
        TaskComment comment = findComment(commentId);
        if (!comment.getAuthorId().equals(currentUserId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        comment.setContent(request.getContent().trim());
        comment = taskCommentRepository.save(comment);

        User author = userRepository.findById(currentUserId).orElse(null);
        return toResponse(comment, author);
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

    // -----------------------------------------------------------------------
    // private helpers
    // -----------------------------------------------------------------------

    private void sendMentionNotifications(String content, Task task, Long authorId) {
        List<String> mentions = parseMentions(content);
        for (String mention : mentions) {
            userRepository.findByMentionStartsWith(mention.toLowerCase()).stream()
                    .filter(u -> !u.getId().equals(authorId))
                    .forEach(u -> notificationService.send(
                            u.getId(), NotificationType.COMMENT_MENTION,
                            "Bạn được nhắc đến trong một bình luận",
                            "Bạn được @mention trong task \"" + task.getTitle() + "\".",
                            "TASK", task.getId()));
        }
    }

    private List<String> parseMentions(String content) {
        if (content == null || !content.contains("@")) return List.of();
        List<String> result = new ArrayList<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            result.add(matcher.group(1).trim());
        }
        return result;
    }

    private void ensureTaskExists(Long taskId) {
        taskRepository.findById(taskId)
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));
    }

    private TaskComment findComment(Long id) {
        return taskCommentRepository.findById(id)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.TASK_COMMENT_NOT_FOUND));
    }

    private TaskCommentResponse toResponse(TaskComment c, User author) {
        return TaskCommentResponse.builder()
                .id(c.getId())
                .taskId(c.getTaskId())
                .author(toUserMeResponse(author))
                .content(c.getContent())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private UserMeResponse toUserMeResponse(User user) {
        return UserMapper.toUserMeResponse(user);
    }
}
