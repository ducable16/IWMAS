package com.roamtrip.entity.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {

    SUCCESS(200, "Success", HttpStatus.OK),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR),

    // --- 9xxx: System & Defaults ---
    UPLOAD_FAILED(9997, "Upload failed", HttpStatus.INTERNAL_SERVER_ERROR),
    URL_NOT_FOUND(9998, "URL not found", HttpStatus.INTERNAL_SERVER_ERROR),
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Invalid error key", HttpStatus.BAD_REQUEST),

    // --- 1xxx: User & Auth ---
    USER_EXISTED(1002, "User already exists", HttpStatus.CONFLICT),
    USERNAME_INVALID(1003, "Username must be at least 3 characters", HttpStatus.BAD_REQUEST),
    USERNAME_ALREADY_EXISTS(1004, "Username already exists", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS(10041, "Email already exists", HttpStatus.CONFLICT),
    PASSWORD_INVALID(1005, "Password must be at least 8 characters", HttpStatus.BAD_REQUEST),
    WRONG_PASSWORD(1006, "Incorrect password", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1007, "User not found", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1008, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1009, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_OTP(1010, "Invalid or expired OTP", HttpStatus.BAD_REQUEST),
    INVALID_INPUT(1011, "Invalid input", HttpStatus.BAD_REQUEST),
    INVALID_FORMAT(1012, "Invalid JSON format", HttpStatus.BAD_REQUEST),
    OTP_INCORRECT(1013, "OTP incorrect", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(1013, "OTP expired", HttpStatus.BAD_REQUEST),

    // --- 2xxx: Book/Domain ---
    BOOK_NOT_FOUND(2001, "Book not found", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND(2002, "Category not found", HttpStatus.NOT_FOUND),
    AUTHOR_NOT_FOUND(2003, "Author not found", HttpStatus.NOT_FOUND),
    CHAPTER_NOT_FOUND(2004, "Chapter not found", HttpStatus.NOT_FOUND),
    BOOK_ALREADY_EXISTS(2011, "Book already exists", HttpStatus.CONFLICT),
    CATEGORY_ALREADY_EXISTS(2012, "Category already exists", HttpStatus.CONFLICT),
    SLUG_ALREADY_EXISTS(2013, "Slug already exists", HttpStatus.CONFLICT),
    COMMENT_NOT_FOUND(2014, "Comment not found", HttpStatus.NOT_FOUND),
    PARENT_COMMENT_NOT_FOUND(2015, "Parent comment not found", HttpStatus.NOT_FOUND),
    HISTORY_NOT_FOUND(2016, "Reading history not found", HttpStatus.NOT_FOUND),
    SAVED_BOOK_NOT_FOUND(2017, "Saved book not found", HttpStatus.NOT_FOUND),
    BOOK_ALREADY_SAVED(2018, "Book already saved", HttpStatus.CONFLICT),

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