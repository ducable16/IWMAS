package com.iwas.common.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {

    SUCCESS(200, "Success", HttpStatus.OK),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR),

    // --- 9xxx: System & Defaults ---
    FILE_TOO_LARGE(9991, "File size exceeds the allowed limit", HttpStatus.BAD_REQUEST),
    FILE_TYPE_NOT_ALLOWED(9992, "File type is not allowed", HttpStatus.BAD_REQUEST),
    ATTACHMENT_NOT_FOUND(9993, "Attachment not found", HttpStatus.NOT_FOUND),
    DOCUMENT_NOT_FOUND(9994, "Document not found", HttpStatus.NOT_FOUND),
    UPLOAD_FAILED(9997, "Upload failed", HttpStatus.INTERNAL_SERVER_ERROR),
    URL_NOT_FOUND(9998, "URL not found", HttpStatus.NOT_FOUND),
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Invalid error key", HttpStatus.BAD_REQUEST),

    // --- 1xxx: Auth & User ---
    USER_EXISTED(1002, "User already exists", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS(1005, "Email already exists", HttpStatus.CONFLICT),
    PASSWORD_INVALID(1006, "Password must be at least 8 characters", HttpStatus.BAD_REQUEST),
    WRONG_PASSWORD(1007, "Incorrect password", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1008, "User not found", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1009, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1010, "You do not have permission", HttpStatus.FORBIDDEN),
    FORBIDDEN(1016, "You are not allowed to perform this action", HttpStatus.FORBIDDEN),
    INVALID_OTP(1011, "Invalid or expired OTP", HttpStatus.BAD_REQUEST),
    INVALID_INPUT(1012, "Invalid input", HttpStatus.BAD_REQUEST),
    INVALID_FORMAT(1013, "Invalid JSON format", HttpStatus.BAD_REQUEST),
    OTP_INCORRECT(1014, "OTP incorrect", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(1015, "OTP expired", HttpStatus.BAD_REQUEST),
    INVALID_ACCESS_TOKEN(1017, "Invalid or expired access token", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_MISSING(1018, "Refresh token is missing", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_INVALID(1019, "Invalid refresh token", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_REVOKED(1020, "Refresh token has been revoked", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED(1021, "Refresh token has expired", HttpStatus.UNAUTHORIZED),

    // --- 3xxx: Skills ---
    SKILL_NOT_FOUND(3001, "Skill not found", HttpStatus.NOT_FOUND),
    SKILL_ALREADY_EXISTS(3002, "Skill already exists", HttpStatus.CONFLICT),
    EMPLOYEE_SKILL_NOT_FOUND(3003, "Employee skill not found", HttpStatus.NOT_FOUND),
    EMPLOYEE_SKILL_ALREADY_EXISTS(3004, "Employee already has this skill", HttpStatus.CONFLICT),
    SKILL_CATEGORY_NOT_FOUND(3005, "Skill category not found", HttpStatus.NOT_FOUND),
    SKILL_CATEGORY_ALREADY_EXISTS(3006, "Skill category already exists", HttpStatus.CONFLICT),
    SKILL_CATEGORY_IN_USE(3007, "Skill category is still referenced by active skills", HttpStatus.CONFLICT),
    SKILL_IN_USE_BY_TASKS(3008, "Skill is still required by one or more active tasks", HttpStatus.CONFLICT),
    SKILL_IN_USE_BY_MEMBERS(3009, "Skill is still assigned to one or more members", HttpStatus.CONFLICT),

    // --- 4xxx: Projects ---
    PROJECT_NOT_FOUND(4001, "Project not found", HttpStatus.NOT_FOUND),
    PROJECT_CODE_ALREADY_EXISTS(4002, "Project code already exists", HttpStatus.CONFLICT),
    PROJECT_CODE_INVALID(4006, "Project code must be 2–10 uppercase letters, digits, or hyphens", HttpStatus.BAD_REQUEST),
    PROJECT_CODE_IMMUTABLE(4007, "Project code cannot be changed after creation", HttpStatus.CONFLICT),
    PROJECT_MEMBER_NOT_FOUND(4003, "Project member not found", HttpStatus.NOT_FOUND),
    PROJECT_MEMBER_ALREADY_EXISTS(4004, "User is already a member of this project", HttpStatus.CONFLICT),
    USER_OVER_ALLOCATED(4005, "User's peak concurrent allocation would exceed 100%", HttpStatus.CONFLICT),
    PROJECT_MEMBER_ALLOC_REQUIRED(4008, "Allocated effort percent is required", HttpStatus.BAD_REQUEST),
    PROJECT_MANAGER_IMMUTABLE(4009, "Project manager cannot be changed after creation", HttpStatus.CONFLICT),
    PROJECT_MANAGER_UNCHANGED(4010, "New manager must be different from the current manager", HttpStatus.BAD_REQUEST),
    PROJECT_MEMBER_ROLE_REQUIRED(4011, "Only users with the Team Member role can join a project as a member", HttpStatus.BAD_REQUEST),
    PROJECT_MANAGER_ROLE_REQUIRED(4012, "Only users with the Project Manager role can be a project's manager", HttpStatus.BAD_REQUEST),

    // --- 5xxx: Tasks ---
    TASK_NOT_FOUND(5001, "Task not found", HttpStatus.NOT_FOUND),
    TASK_INVALID_STATUS_TRANSITION(5002, "Invalid task status transition", HttpStatus.BAD_REQUEST),
    TASK_COMMENT_NOT_FOUND(5003, "Task comment not found", HttpStatus.NOT_FOUND),
    TASK_ASSIGNEE_NOT_PROJECT_MEMBER(5004, "Assignee is not a member of this project", HttpStatus.BAD_REQUEST),
    TASK_INVALID_DATE_RANGE(5005, "Start date must not be after due date", HttpStatus.BAD_REQUEST),
    TASK_DATES_REQUIRED(5006, "At least one of start date or due date is required", HttpStatus.BAD_REQUEST),
    TASK_ASSIGNEE_SKILL_NOT_MET(5007, "Assignee does not meet the required skill level for this task", HttpStatus.BAD_REQUEST),
    TASK_ASSIGNEE_CANNOT_BE_MANAGER(5008, "Project manager cannot be assigned tasks", HttpStatus.BAD_REQUEST),

    // --- 7xxx: Notifications ---
    NOTIFICATION_NOT_FOUND(7001, "Notification not found", HttpStatus.NOT_FOUND),

    // --- 95xx: Search ---
    SEARCH_QUERY_TOO_SHORT(9501, "Search query too short", HttpStatus.BAD_REQUEST),
    SEARCH_BACKEND_UNAVAILABLE(9502, "Search backend unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    SEARCH_INDEX_FAILED(9503, "Failed to index document", HttpStatus.INTERNAL_SERVER_ERROR),
    SEARCH_INVALID_SKILL_FILTER(9504, "Invalid requiredSkills filter", HttpStatus.BAD_REQUEST),
    ;

    private final int code;
    private final String message;
    private final HttpStatusCode httpStatus;

    ErrorCode(int code, String message, HttpStatusCode httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
