package com.enterprise.aiassistant.backend.common.exception.business_exception;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;

public class SearchException extends BusinessException {

    public SearchException(ErrorCode errorCode) {
        super(errorCode);
    }

}
