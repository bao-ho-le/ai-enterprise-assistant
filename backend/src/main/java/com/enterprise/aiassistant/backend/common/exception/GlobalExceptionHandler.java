package com.enterprise.aiassistant.backend.common.exception;

import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDto> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ){

        ErrorCode errorCode = exception.getErrorCode();

        ErrorResponseDto response = ErrorResponseDto.builder()
                        .timestamp(LocalDateTime.now())
                        .status(errorCode.getStatus().value())
                        .error(errorCode.name())
                        .message(exception.getMessage())
                        .path(request.getRequestURI())
                        .build();


        return ResponseEntity
                .status(errorCode.getStatus())
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUnknownException(
            Exception exception,
            HttpServletRequest request
    ){

        ErrorResponseDto response =
                ErrorResponseDto.builder()
                        .timestamp(LocalDateTime.now())
                        .status(500)
                        .error("INTERNAL_SERVER_ERROR")
                        .message("Something went wrong")
                        .path(request.getRequestURI())
                        .build();


        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
