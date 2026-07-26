package com.enterprise.aiassistant.backend.generated.helper;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.GeneratedException;
import com.enterprise.aiassistant.backend.generated.dto.request.UpdateGeneratedContentRequest;
import com.enterprise.aiassistant.backend.generated.enums.GeneratedDocumentType;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class GeneratedHelper {

    private static final int MAX_TITLE_LENGTH = 500;
    private static final int MAX_PAGE_SIZE = 50;

    public void validateGeneratedContentId(Long generatedContentId) {
        if (generatedContentId == null) {
            throw new GeneratedException(ErrorCode.GENERATED_CONTENT_ID_REQUIRED);
        }

        if (generatedContentId <= 0) {
            throw new GeneratedException(ErrorCode.GENERATED_CONTENT_ID_INVALID);
        }
    }

    public void validatePageable(Pageable pageable) {
        if (pageable == null) {
            throw new GeneratedException(ErrorCode.PAGEABLE_REQUIRED);
        }

        if (pageable.getPageNumber() < 0) {
            throw new GeneratedException(ErrorCode.PAGE_NUMBER_INVALID);
        }

        if (pageable.getPageSize() <= 0
                || pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new GeneratedException(ErrorCode.PAGE_SIZE_INVALID);
        }
    }

    public void validateCreateData(
            Long aiConversationId,
            GeneratedDocumentType generatedType,
            String title,
            String content
    ) {
        if (aiConversationId == null) {
            throw new GeneratedException(ErrorCode.AI_CONVERSATION_ID_REQUIRED);
        }

        if (generatedType == null) {
            throw new GeneratedException(ErrorCode.GENERATED_CONTENT_TYPE_REQUIRED);
        }

        validateTitle(title);
        validateContent(content);
    }

    public void validateUpdateRequest(UpdateGeneratedContentRequest request) {
        if (request == null) {
            throw new GeneratedException(ErrorCode.GENERATED_CONTENT_UPDATE_REQUEST_REQUIRED);
        }

        validateTitle(request.getTitle());
        validateContent(request.getContent());
    }

    public void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new GeneratedException(ErrorCode.GENERATED_CONTENT_TITLE_REQUIRED);
        }

        if (title.trim().length() > MAX_TITLE_LENGTH) {
            throw new GeneratedException(ErrorCode.GENERATED_CONTENT_TITLE_TOO_LONG);
        }
    }

    public void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new GeneratedException(ErrorCode.GENERATED_CONTENT_BODY_REQUIRED);
        }
    }
}
