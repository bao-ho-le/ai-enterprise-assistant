package com.enterprise.aiassistant.backend.ai.conversation.service;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.AttachDocumentsRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.CreateConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.UpdateConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationDocumentResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationResponse;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversationDocument;
import com.enterprise.aiassistant.backend.ai.conversation.helper.ConversationHelper;
import com.enterprise.aiassistant.backend.ai.conversation.mapper.ConversationMapper;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIConversationDocumentRepository;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIConversationRepository;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIMessageRepository;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIMessageSourceRepository;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ConversationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.DocumentException;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.repository.DocumentVersionRepository;
import com.enterprise.aiassistant.backend.generated.dto.response.GeneratedContentResponse;
import com.enterprise.aiassistant.backend.generated.mapper.GeneratedMapper;
import com.enterprise.aiassistant.backend.generated.repository.GeneratedContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AIConversationServiceImpl implements AIConversationService {

    private final AIConversationRepository conversationRepository;

    private final AIConversationDocumentRepository conversationDocumentRepository;

    private final AIMessageRepository messageRepository;

    private final AIMessageSourceRepository messageSourceRepository;

    private final DocumentVersionRepository documentVersionRepository;

    private final GeneratedContentRepository generatedContentRepository;

    private final ConversationMapper conversationMapper;

    private final GeneratedMapper generatedMapper;

    private final ConversationHelper conversationHelper;

    @Override
    @Transactional
    public ConversationResponse createConversation(CreateConversationRequest request) {

        conversationHelper.validateCreateConversationRequest(request);

        AIConversation conversation = conversationMapper.toEntity(request);
        conversationRepository.save(conversation);

        return conversationMapper.toResponse(conversation);
    }

    @Override
    @Transactional
    public ConversationResponse updateConversation(Long conversationId, UpdateConversationRequest request) {

        AIConversation conversation = getActiveConversationOrThrow(conversationId);

        conversation.setTitle(request.getTitle());
        conversationRepository.save(conversation);

        return conversationMapper.toResponse(conversation);
    }

    @Override
    @Transactional
    public void softDeleteConversation(Long conversationId) {

        AIConversation conversation = getActiveConversationOrThrow(conversationId);

        conversation.setDeleted(true);
        conversation.setDeletedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    @Override
    @Transactional
    public void hardDeleteConversation(Long conversationId) {

        // Hard delete must reach conversations already soft-deleted too, so no deletedFalse filter here.
        AIConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationException(ErrorCode.CONVERSATION_NOT_FOUND));

        messageSourceRepository.deleteByMessage_ConversationId(conversationId);
        messageRepository.deleteByConversationId(conversationId);
        conversationDocumentRepository.deleteByAiConversationId(conversationId);
        generatedContentRepository.deleteByAiConversationId(conversationId);
        conversationRepository.delete(conversation);
    }

    @Override
    @Transactional
    public ConversationDetailResponse attachDocuments(Long conversationId, AttachDocumentsRequest request) {

        AIConversation conversation = getActiveConversationOrThrow(conversationId);

        List<Long> versionIds = request.getDocumentVersionIds().stream().distinct().toList();
        List<DocumentVersion> versions = documentVersionRepository.findAllById(versionIds);

        if (versions.size() != versionIds.size()) {
            throw new DocumentException(ErrorCode.DOCUMENT_VERSION_NOT_FOUND);
        }

        List<Long> alreadyAttachedIds =
                conversationDocumentRepository.findDocumentVersionIdsByAiConversationId(conversationId);

        List<DocumentVersion> newVersions =
                conversationHelper.filterNewVersions(versions, alreadyAttachedIds);

        List<AIConversationDocument> newLinks = newVersions.stream()
                .map(version -> conversationMapper.toConversationDocument(conversation, version))
                .toList();

        conversationDocumentRepository.saveAll(newLinks);

        List<AIConversationDocument> allLinks =
                conversationDocumentRepository.findByAiConversationIdWithDocument(conversationId);

        return conversationMapper.toDetailResponse(conversation, allLinks);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationDocumentResponse> getConversationDocuments(Long conversationId) {

        getActiveConversationOrThrow(conversationId);

        return conversationDocumentRepository
                .findByAiConversationIdWithDocument(conversationId)
                .stream()
                .map(conversationMapper::toConversationDocumentResponse)
                .toList();
    }

    @Override
    @Transactional
    public void removeDocument(Long conversationId, Long documentVersionId) {

        getActiveConversationOrThrow(conversationId);

        AIConversationDocument conversationDocument = conversationDocumentRepository
                .findByAiConversationIdAndDocumentVersionId(conversationId, documentVersionId)
                .orElseThrow(() -> new ConversationException(ErrorCode.DOCUMENT_NOT_ATTACHED_TO_CONVERSATION));

        conversationDocumentRepository.delete(conversationDocument);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<GeneratedContentResponse> getConversationGeneratedContents(
            Long conversationId,
            Pageable pageable
    ) {

        getActiveConversationOrThrow(conversationId);

        return generatedContentRepository
                .findByAiConversationIdOrderByCreatedAtDesc(conversationId, pageable)
                .map(generatedMapper::toGeneratedContentResponse);
    }

    private AIConversation getActiveConversationOrThrow(Long conversationId) {
        return conversationRepository.findByIdAndDeletedFalse(conversationId)
                .orElseThrow(() -> new ConversationException(ErrorCode.CONVERSATION_NOT_FOUND));
    }
}
