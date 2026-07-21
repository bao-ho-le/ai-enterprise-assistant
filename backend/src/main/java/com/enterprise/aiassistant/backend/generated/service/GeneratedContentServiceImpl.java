
package com.enterprise.aiassistant.backend.generated.service;


import com.enterprise.aiassistant.backend.generated.dto.request.UpdateGeneratedContentRequest;
import com.enterprise.aiassistant.backend.generated.dto.response.GeneratedContentDetailResponse;
import com.enterprise.aiassistant.backend.generated.dto.response.GeneratedContentResponse;
import com.enterprise.aiassistant.backend.generated.entity.GeneratedContent;
import com.enterprise.aiassistant.backend.generated.enums.GeneratedDocumentType;
import com.enterprise.aiassistant.backend.generated.helper.GeneratedHelper;
import com.enterprise.aiassistant.backend.generated.mapper.GeneratedMapper;
import com.enterprise.aiassistant.backend.generated.repository.GeneratedContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GeneratedContentServiceImpl implements GeneratedContentService {

    private final GeneratedContentRepository generatedContentRepository;
    private final GeneratedMapper generatedMapper;
    private final GeneratedHelper generatedHelper;

    @Override
    @Transactional(readOnly = true)
    public Slice<GeneratedContentResponse> getGeneratedContents(
            GeneratedDocumentType generatedType,
            Pageable pageable
    ) {
        Slice<GeneratedContent> generatedContents;

        generatedHelper.validatePageable(pageable);

        if (generatedType == null) {
            generatedContents =
                    generatedContentRepository
                            .findAllByOrderByCreatedAtDesc(pageable);
        } else {
            generatedContents =
                    generatedContentRepository
                            .findByGeneratedTypeOrderByCreatedAtDesc(
                                    generatedType,
                                    pageable
                            );
        }

        return generatedContents.map(generatedMapper::toGeneratedContentResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public GeneratedContentDetailResponse getGeneratedContentById(Long id) {
        generatedHelper.validateGeneratedContentId(id);

        GeneratedContent generatedContent = generatedHelper.findGeneratedContentById(id);

        return generatedMapper.toGeneratedContentDetailResponse(generatedContent);
    }

    @Override
    @Transactional
    public GeneratedContentDetailResponse updateGeneratedContent(
            Long id,
            UpdateGeneratedContentRequest request
    ) {
        generatedHelper.validateGeneratedContentId(id);
        generatedHelper.validateUpdateRequest(request);

        GeneratedContent generatedContent = generatedHelper.findGeneratedContentById(id);

        generatedContent.setTitle(request.getTitle().trim());
        generatedContent.setContent(request.getContent().trim());

        return generatedMapper.toGeneratedContentDetailResponse(generatedContent);
    }

    @Override
    @Transactional
    public GeneratedContentDetailResponse createGeneratedContent(
            Long aiConversationId,
            GeneratedDocumentType generatedType,
            String title,
            String content
    ) {


        generatedHelper.validateCreateData(
                aiConversationId,
                generatedType,
                title,
                content
        );

        GeneratedContent generatedContent = generatedMapper.toCreateGeneratedContentObject(
                aiConversationId,
                generatedType,
                title,
                content
        );

        GeneratedContent savedContent =
                generatedContentRepository.save(generatedContent);

        return generatedMapper.toGeneratedContentDetailResponse(savedContent);
    }



}

