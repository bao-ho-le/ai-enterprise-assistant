package com.enterprise.aiassistant.backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Storage
    FILE_UPLOAD_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "File upload failed"
    ),

    EMPTY_FILE(
            HttpStatus.BAD_REQUEST,
            "File cannot be empty"
    ),


    // Document
    DOCUMENT_CREATION_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Document creation failed"
    ),

    DOCUMENT_VERSION_CREATION_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Document version creation failed"
    ),

    DOCUMENT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Document not found"
    );

    private final HttpStatus status;
    private final String message;


    ErrorCode(
            HttpStatus status,
            String message
    ){
        this.status = status;
        this.message = message;
    }
}
