package com.enterprise.aiassistant.backend.common.exception.business_exception;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;

public class VectorStoreException extends BusinessException {

    public VectorStoreException(ErrorCode errorCode) {
        super(errorCode);
    }

    public VectorStoreException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, errorCode.getMessage(), cause);
    }

}
