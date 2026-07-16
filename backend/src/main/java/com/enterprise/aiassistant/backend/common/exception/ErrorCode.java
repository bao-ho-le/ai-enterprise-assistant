package com.enterprise.aiassistant.backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Getter
public enum ErrorCode {

    REQUEST_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "Request body is required"
    ),

    // Storage
    FILE_UPLOAD_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "File upload failed"
    ),

    FILE_STORAGE_READ_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "File storage read failed"
    ),

    FILE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "File not found"
    ),

    EMPTY_FILE(
            BAD_REQUEST,
            "File cannot be empty"
    ),

    FILE_TOO_LARGE(
            BAD_REQUEST,
            "File size exceeds limit"
    ),

    FILE_REQUIRED(
            BAD_REQUEST,
            "File is required"
    ),

    UNSUPPORTED_FILE_TYPE(
            BAD_REQUEST,
            "Unsupported file type"
    ),

    FILE_STORAGE_METADATA_INVALID(
            BAD_REQUEST,
            "File storage metadata is invalid"
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

    DOCUMENT_ID_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "Document id is required"
    ),

    DOCUMENT_DELETED(
            HttpStatus.BAD_REQUEST,
            "Document has been deleted"
    ),

    DOCUMENT_TITLE_REQUIRED(
            BAD_REQUEST,
            "Document title is required"
    ),

    DOCUMENT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Document not found"
    ),

    DOCUMENT_HAS_NO_CURRENT_VERSION(
            HttpStatus.BAD_REQUEST,
            "Document has no current version"
    ),

    DOCUMENT_VERSION_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Document version not found"
    ),

    INVALID_DOCUMENT_TYPE(
            HttpStatus.BAD_REQUEST,
            "Invalid document type"
    ),





    // Document Version

    CHANGE_NOTE_TOO_LONG(
            HttpStatus.BAD_REQUEST,
            "Change note exceeds maximum length"
    ),
    TITLE_TOO_LONG(
            HttpStatus.BAD_REQUEST,
            "Document title exceeds maximum length"
    ),

    DESCRIPTION_TOO_LONG(
            HttpStatus.BAD_REQUEST,
            "Document description exceeds maximum length"
    ),;


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
