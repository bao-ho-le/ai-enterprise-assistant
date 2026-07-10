package com.enterprise.aiassistant.backend.common.exception.business_exception;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;

public class FileStorageException extends BusinessException {
    public FileStorageException(
            String message,
            Throwable cause
    ){
        super(
                ErrorCode.FILE_UPLOAD_FAILED,
                message,
                cause
        );
    }
}
