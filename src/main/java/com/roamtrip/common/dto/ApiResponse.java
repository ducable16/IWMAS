package com.roamtrip.common.dto;

import com.roamtrip.common.enums.ErrorCode;
import com.roamtrip.common.enums.ResponseCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

    private int code = ResponseCode.SUCCESS;
    private String message;
    private T data;

    public ApiResponse() {
        this.code = ResponseCode.SUCCESS;
    }

    public ApiResponse(T data) {
        this.data = data;
    }

    public ApiResponse(int code) {
        this.code = code;
    }

    public ApiResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static <T> ApiResponse<T> success(T result) {
        return ApiResponse.<T>builder()
                .code(ResponseCode.SUCCESS)
                .message("Success")
                .data(result)
                .build();
    }

    public static <T> ApiResponse<T> success() {
        return ApiResponse.<T>builder()
                .code(ResponseCode.SUCCESS)
                .message("Success")
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return ApiResponse.<T>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String customMessage) {
        return ApiResponse.<T>builder()
                .code(errorCode.getCode())
                .message(customMessage)
                .build();
    }
}
