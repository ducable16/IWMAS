package com.iwas.common.enums;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ResponseCode {
    public static final int SUCCESS = 200;
    public static final int INVALID_PASSWORD = 102;
    public static final int PASSWORDS_DO_NOT_MATCH = 105;
    public static final int USER_NOT_FOUND = 101;
    public static final int EMAIL_ALREADY_EXISTS = 103;
    public static final int INVALID_TOKEN = 104;
    public static final int USER_ALREADY_EXISTS = 100;
    public static final int WRONG_DATA_FORMAT = 400;
    public static final int UNAUTHORIZED_ERROR = 401;
    public static final int ACCESS_DENIED = 403;
    public static final int NOT_FOUND = 404;
    public static final int INTERNAL_SERVER_ERROR = 500;
    public static final int OTP_EXPIRED = 410;
    public static final int OTP_INCORRECT = 411;
}
