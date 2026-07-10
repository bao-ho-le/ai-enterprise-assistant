package com.enterprise.aiassistant.backend.common.exception.business_exception;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;

public class DocumentException extends BusinessException {

    public DocumentException(ErrorCode errorCode) {
        super(
                errorCode,
                errorCode.getMessage()
        );
    }
}
