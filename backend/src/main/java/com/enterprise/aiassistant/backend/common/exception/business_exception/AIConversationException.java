package com.enterprise.aiassistant.backend.common.exception.business_exception;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;

public class AIConversationException extends BusinessException {

    public AIConversationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AIConversationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, errorCode.getMessage(), cause);
    }
}
