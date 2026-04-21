package com.iwas.common.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {

    SUCCESS(200, "Success", HttpStatus.OK),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR),

    // --- 9xxx: System & Defaults ---
    UPLOAD_FAILED(9997, "Upload failed", HttpStatus.INTERNAL_SERVER_ERROR),
    URL_NOT_FOUND(9998, "URL not found", HttpStatus.NOT_FOUND),
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Invalid error key", HttpStatus.BAD_REQUEST),

    // --- 1xxx: Auth & User ---
    USER_EXISTED(1002, "User already exists", HttpStatus.CONFLICT),
    USERNAME_INVALID(1003, "Username must be at least 3 characters", HttpStatus.BAD_REQUEST),
    USERNAME_ALREADY_EXISTS(1004, "Username already exists", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS(1005, "Email already exists", HttpStatus.CONFLICT),
    PASSWORD_INVALID(1006, "Password must be at least 8 characters", HttpStatus.BAD_REQUEST),
    WRONG_PASSWORD(1007, "Incorrect password", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1008, "User not found", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1009, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1010, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_OTP(1011, "Invalid or expired OTP", HttpStatus.BAD_REQUEST),
    INVALID_INPUT(1012, "Invalid input", HttpStatus.BAD_REQUEST),
    INVALID_FORMAT(1013, "Invalid JSON format", HttpStatus.BAD_REQUEST),
    OTP_INCORRECT(1014, "OTP incorrect", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(1015, "OTP expired", HttpStatus.BAD_REQUEST),

    // --- 2xxx: Department ---
    DEPARTMENT_NOT_FOUND(2001, "Department not found", HttpStatus.NOT_FOUND),
    DEPARTMENT_ALREADY_EXISTS(2002, "Department already exists", HttpStatus.CONFLICT),

    // --- 3xxx: Skills ---
    SKILL_NOT_FOUND(3001, "Skill not found", HttpStatus.NOT_FOUND),
    SKILL_ALREADY_EXISTS(3002, "Skill already exists", HttpStatus.CONFLICT),
    EMPLOYEE_SKILL_NOT_FOUND(3003, "Employee skill not found", HttpStatus.NOT_FOUND),
    EMPLOYEE_SKILL_ALREADY_EXISTS(3004, "Employee already has this skill", HttpStatus.CONFLICT),

    // --- 4xxx: Projects ---
    PROJECT_NOT_FOUND(4001, "Project not found", HttpStatus.NOT_FOUND),
    PROJECT_CODE_ALREADY_EXISTS(4002, "Project code already exists", HttpStatus.CONFLICT),
    PROJECT_MEMBER_NOT_FOUND(4003, "Project member not found", HttpStatus.NOT_FOUND),
    PROJECT_MEMBER_ALREADY_EXISTS(4004, "User is already a member of this project", HttpStatus.CONFLICT),

    // --- 5xxx: Tasks ---
    TASK_NOT_FOUND(5001, "Task not found", HttpStatus.NOT_FOUND),
    TASK_INVALID_STATUS_TRANSITION(5002, "Invalid task status transition", HttpStatus.BAD_REQUEST),

    // --- 6xxx: Time Logs ---
    TIME_LOG_NOT_FOUND(6001, "Time log not found", HttpStatus.NOT_FOUND),
    TIME_LOG_ALREADY_EXISTS(6002, "Time log already exists for this task/user/date", HttpStatus.CONFLICT),

    // --- 7xxx: Notifications ---
    NOTIFICATION_NOT_FOUND(7001, "Notification not found", HttpStatus.NOT_FOUND),

    // --- 8xxx: Workload ---
    WORKLOAD_SNAPSHOT_NOT_FOUND(8001, "Workload snapshot not found", HttpStatus.NOT_FOUND),
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
