package com.enterprise.aiassistant.backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
public enum ErrorCode {

    REQUEST_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "Request body is required"
    ),

    // File
    FILE_UPLOAD_FAILED(
            INTERNAL_SERVER_ERROR,
            "File upload failed"
    ),

    FILE_STORAGE_READ_FAILED(
            INTERNAL_SERVER_ERROR,
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

    TEXT_EXTRACTOR_NOT_FOUND(
            INTERNAL_SERVER_ERROR,
            "No text extractor found for the file type"
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
            INTERNAL_SERVER_ERROR,
            "Document creation failed"
    ),

    DOCUMENT_VERSION_CREATION_FAILED(
            INTERNAL_SERVER_ERROR,
            "Document version creation failed"
    ),

    DOCUMENT_VERSION_INVALID_STATUS(
            CONFLICT,
            "Document version is not in a valid state for processing"
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

    DOCUMENT_ID_INVALID(
            BAD_REQUEST,
            "Document id is invalid"
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
    ),

    // ===================== Processing =====================

    TEXT_EXTRACTION_FAILED(
            INTERNAL_SERVER_ERROR,
            "Failed to extract text from the document"
    ),

    DOCUMENT_CHUNKING_FAILED(
            INTERNAL_SERVER_ERROR,
            "Failed to split the document into chunks"
    ),

    DOCUMENT_TEXT_EMPTY(
            INTERNAL_SERVER_ERROR,
            "No extractable text found in the document"
    ),

    DOCUMENT_PROCESSING_FAILED(
            INTERNAL_SERVER_ERROR,
            "Failed to process the document"
    ),

    // ===================== Document Filter =====================

    INVALID_DATE_RANGE(
            BAD_REQUEST,
            "From date must be before or equal to to date"
    ),

    INVALID_FILE_SIZE(
            BAD_REQUEST,
            "File size must be greater than or equal to 0"
    ),

    INVALID_FILE_SIZE_RANGE(
            BAD_REQUEST,
            "Minimum file size must be less than or equal to maximum file size"
    ),

    INVALID_SORT_OPTION(
            BAD_REQUEST,
            "Sort option must be either 'newest' or 'oldest'"
    ),

    KEYWORD_TOO_LONG(
            BAD_REQUEST,
            "Keyword exceeds maximum length"
    ),

    // ===================== Embedding =====================

    EMBEDDING_TEXT_REQUIRED(
            BAD_REQUEST,
            "Text to embed is required"
    ),

    EMBEDDING_FAILED(
            INTERNAL_SERVER_ERROR,
            "Failed to generate embedding"
    ),

    // ===================== Vector Store =====================

    VECTOR_UPSERT_FAILED(
            INTERNAL_SERVER_ERROR,
            "Failed to save vectors to the vector store"
    ),

    VECTOR_DELETE_FAILED(
            INTERNAL_SERVER_ERROR,
            "Failed to delete vectors from the vector store"
    ),

    VECTOR_SEARCH_FAILED(
            INTERNAL_SERVER_ERROR,
            "Failed to search the vector store"
    ),

    VECTOR_POINTS_REQUIRED(
            BAD_REQUEST,
            "Vector points must not be null"
    ),

    VECTOR_POINT_REQUIRED(
            BAD_REQUEST,
            "Vector point must not be null"
    ),

    VECTOR_POINT_ID_REQUIRED(
            BAD_REQUEST,
            "Vector point id must not be blank"
    ),

    VECTOR_POINT_ID_INVALID(
            BAD_REQUEST,
            "Vector point id must be a valid number"
    ),

    VECTOR_REQUIRED(
            BAD_REQUEST,
            "Vector must not be null or empty"
    ),

    VECTOR_DIMENSION_INVALID(
            BAD_REQUEST,
            "Vector dimension is invalid"
    ),

    // ===================== Search =====================

    SEARCH_KEYWORD_REQUIRED(
            BAD_REQUEST,
            "Search keyword is required"
    ),

    INVALID_TOP_K(
            BAD_REQUEST,
            "topK must be between 1 and 50"
    ),

    VECTOR_SEARCH_LIMIT_INVALID(
            BAD_REQUEST,
            "Search limit is invalid"
    ),

    INVALID_DOCUMENT_ID(
            BAD_REQUEST,
            "documentId must be a positive number"
    ),

    // ===================== Generated Document =====================

    GENERATED_CONTENT_NOT_FOUND(
            NOT_FOUND,
            "Generated content not found"
    ),

    GENERATED_CONTENT_ID_REQUIRED(
            BAD_REQUEST,
            "Generated content ID is required"
    ),

    GENERATED_CONTENT_TYPE_REQUIRED(
            BAD_REQUEST,
            "Generated content type is required"
    ),

    GENERATED_CONTENT_TITLE_REQUIRED(
            BAD_REQUEST,
            "Generated content title is required"
    ),

    GENERATED_CONTENT_TITLE_TOO_LONG(
            BAD_REQUEST,
            "Generated content title must not exceed 500 characters"
    ),

    GENERATED_CONTENT_BODY_REQUIRED(
            BAD_REQUEST,
            "Generated content is required"
    ),

    GENERATED_CONTENT_ID_INVALID(
            BAD_REQUEST,
            "Generated content ID must be greater than 0"
    ),

    AI_CONVERSATION_ID_INVALID(
            BAD_REQUEST,
            "AI conversation ID must be greater than 0"
    ),

    GENERATED_CONTENT_UPDATE_REQUEST_REQUIRED(
            BAD_REQUEST,
            "Generated content update request is required"
    ),

    PAGEABLE_REQUIRED(
            BAD_REQUEST,
            "Pagination information is required"
    ),

    PAGE_NUMBER_INVALID(
            BAD_REQUEST,
            "Page number must not be negative"
    ),

    PAGE_SIZE_INVALID(
            BAD_REQUEST,
            "Page size must be between 1 and 50"
    ),

    AI_CONVERSATION_ID_REQUIRED(
            BAD_REQUEST,
            "AI conversation ID is required"
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