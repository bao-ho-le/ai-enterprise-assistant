
package com.enterprise.aiassistant.backend.generated.helper;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.GeneratedException;
import com.enterprise.aiassistant.backend.generated.dto.request.UpdateGeneratedContentRequest;
import com.enterprise.aiassistant.backend.generated.entity.GeneratedContent;
import com.enterprise.aiassistant.backend.generated.enums.GeneratedDocumentType;
import com.enterprise.aiassistant.backend.generated.repository.GeneratedContentRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class GeneratedHelper {
    private final GeneratedContentRepository generatedContentRepository;

    private static final int MAX_TITLE_LENGTH = 500;
    private static final int MAX_PAGE_SIZE = 50;

    /**
     * Dùng cho:
     * - getGeneratedContentById(Long id)
     * - updateGeneratedContent(Long id, UpdateGeneratedContentRequest request)
     *
     * Kiểm tra ID không null và phải lớn hơn 0.
     */
    public void validateGeneratedContentId(Long id) {
        if (id == null) {
            throw new GeneratedException(
                    ErrorCode.GENERATED_CONTENT_ID_REQUIRED
            );
        }

        if (id <= 0) {
            throw new GeneratedException(
                    ErrorCode.GENERATED_CONTENT_ID_INVALID
            );
        }
    }

    /**
     * Dùng cho:
     * - getGeneratedContents(GeneratedDocumentType generatedType, Pageable pageable)
     *
     * Kiểm tra thông tin phân trang hợp lệ.
     * generatedType không cần kiểm tra vì đây là filter tùy chọn.
     */
    public void validatePageable(Pageable pageable) {
        if (pageable == null) {
            throw new GeneratedException(
                    ErrorCode.PAGEABLE_REQUIRED
            );
        }

        if (pageable.getPageNumber() < 0) {
            throw new GeneratedException(
                    ErrorCode.PAGE_NUMBER_INVALID
            );
        }

        if (pageable.getPageSize() <= 0
                || pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new GeneratedException(
                    ErrorCode.PAGE_SIZE_INVALID
            );
        }
    }

   /**
    * Dùng cho:
    * - createGeneratedContent(...)
    *
    * Kiểm tra toàn bộ dữ liệu trước khi tạo generated content.
    */

    public void validateCreateData(
            Long aiConversationId,
            GeneratedDocumentType generatedType,
            String title,
            String content
    ) {
        if (aiConversationId == null) {
            throw new GeneratedException(
                    ErrorCode.AI_CONVERSATION_ID_REQUIRED
            );
        }

        if (generatedType == null) {
            throw new GeneratedException(
                    ErrorCode.GENERATED_CONTENT_TYPE_REQUIRED
            );
        }

        if (title == null || title.isBlank()) {
            throw new GeneratedException(
                    ErrorCode.GENERATED_CONTENT_TITLE_REQUIRED
            );
        }

        if (title.trim().length() > 500) {
            throw new GeneratedException(
                    ErrorCode.GENERATED_CONTENT_TITLE_TOO_LONG
            );
        }

        if (content == null || content.isBlank()) {
            throw new GeneratedException(
                    ErrorCode.GENERATED_CONTENT_BODY_REQUIRED
            );
        }
    }

    /**
     * Dùng cho:
     * - updateGeneratedContent(Long id, UpdateGeneratedContentRequest request)
     *
     * Kiểm tra request update và các field được phép chỉnh sửa.
     */
    public void validateUpdateRequest(
            UpdateGeneratedContentRequest request
    ) {
        if (request == null) {
            throw new GeneratedException(
                    ErrorCode.GENERATED_CONTENT_UPDATE_REQUEST_REQUIRED
            );
        }

        validateTitle(request.getTitle());
        validateContent(request.getContent());
    }

    /**
     * Dùng cho:
     * - createGeneratedContent(...)
     * - updateGeneratedContent(...)
     */
    public void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new GeneratedException(
                    ErrorCode.GENERATED_CONTENT_TITLE_REQUIRED
            );
        }

        if (title.trim().length() > MAX_TITLE_LENGTH) {
            throw new GeneratedException(
                    ErrorCode.GENERATED_CONTENT_TITLE_TOO_LONG
            );
        }
    }

    /**
     * Dùng cho:
     * - createGeneratedContent(...)
     * - updateGeneratedContent(...)
     */
    public void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new GeneratedException(
                    ErrorCode.GENERATED_CONTENT_BODY_REQUIRED
            );
        }
    }

    public GeneratedContent findGeneratedContentById(Long id) {
        if (id == null) {
            throw new GeneratedException(
                    ErrorCode.GENERATED_CONTENT_ID_REQUIRED
            );
        }

        return generatedContentRepository.findById(id)
                .orElseThrow(() -> new GeneratedException(
                        ErrorCode.GENERATED_CONTENT_NOT_FOUND
                ));
    }

}

