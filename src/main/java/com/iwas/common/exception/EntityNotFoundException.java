package com.iwas.common.exception;

import com.iwas.common.enums.ErrorCode;

public class EntityNotFoundException extends AppException {

    public EntityNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public EntityNotFoundException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}
