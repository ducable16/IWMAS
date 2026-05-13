package com.iwas.common.storage;

import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Component
public class FileValidator {

    private static final long AVATAR_MAX_BYTES = 2L * 1024 * 1024;
    private static final long ATTACHMENT_MAX_BYTES = 20L * 1024 * 1024;

    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private static final Set<String> ATTACHMENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain"
    );

    public void validateAvatar(MultipartFile file) {
        if (file.getSize() > AVATAR_MAX_BYTES) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }
        if (!IMAGE_TYPES.contains(file.getContentType())) {
            throw new AppException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
    }

    public void validateAttachment(MultipartFile file) {
        if (file.getSize() > ATTACHMENT_MAX_BYTES) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }
        if (!ATTACHMENT_TYPES.contains(file.getContentType())) {
            throw new AppException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
    }
}
