package com.enterprise.aiassistant.backend.common.exception;

import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDto> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ){

        ErrorCode errorCode = exception.getErrorCode();

        ErrorResponseDto response = ErrorResponseDto.builder()
                        .timestamp(java.time.LocalDateTime.now())
                        .status(errorCode.getStatus().value())
                        .error(errorCode.name())
                        .message(exception.getMessage())
                        .path(request.getRequestURI())
                        .build();


        return ResponseEntity
                .status(errorCode.getStatus())
                .body(response);
    }
}
