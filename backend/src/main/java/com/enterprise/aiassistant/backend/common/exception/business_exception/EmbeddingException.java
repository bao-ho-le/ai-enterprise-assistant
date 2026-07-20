package com.enterprise.aiassistant.backend.common.exception.business_exception;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;

public class EmbeddingException extends BusinessException {

    public EmbeddingException(ErrorCode errorCode) {
        super(errorCode);
    }

    public EmbeddingException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, errorCode.getMessage(), cause);
    }

}
