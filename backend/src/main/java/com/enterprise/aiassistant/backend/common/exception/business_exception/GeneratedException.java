package com.enterprise.aiassistant.backend.common.exception.business_exception;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;

public class GeneratedException extends BusinessException {

    public GeneratedException(ErrorCode errorCode) {
        super(errorCode);
    }

    public GeneratedException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, errorCode.getMessage(), cause);
    }

}