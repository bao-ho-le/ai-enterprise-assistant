
package com.enterprise.aiassistant.backend.generated.service;

import com.enterprise.aiassistant.backend.generated.dto.request.UpdateGeneratedContentRequest;
import com.enterprise.aiassistant.backend.generated.dto.response.GeneratedContentDetailResponse;
import com.enterprise.aiassistant.backend.generated.dto.response.GeneratedContentResponse;
import com.enterprise.aiassistant.backend.generated.enums.GeneratedDocumentType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface GeneratedContentService {

    Slice<GeneratedContentResponse> getGeneratedContents(
            GeneratedDocumentType generatedType,
            Pageable pageable
    );

    GeneratedContentDetailResponse getGeneratedContentById(Long id);

    GeneratedContentDetailResponse updateGeneratedContent(
            Long id,
            UpdateGeneratedContentRequest request
    );

    GeneratedContentDetailResponse createGeneratedContent(
            Long aiConversationId,
            GeneratedDocumentType generatedType,
            String title,
            String content
    );
}

