package com.enterprise.aiassistant.backend.common.exception.business_exception;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;

public class ProcessingException extends BusinessException {
  public ProcessingException(
          ErrorCode errorCode
  ){
    super(
            errorCode
    );
  }

  public ProcessingException(
          ErrorCode errorCode,
          Throwable cause
  ){
    super(
            errorCode,
            errorCode.getMessage()
    );
  }

  public ProcessingException(
          ErrorCode errorCode,
          String message,
          Throwable cause
  ){
    super(
            errorCode,
            message,
            cause
    );
  }

}