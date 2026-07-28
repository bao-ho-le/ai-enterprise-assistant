package com.enterprise.aiassistant.backend.ai.conversation.service;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.AttachDocumentsRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.ConversationFilterRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.CreateConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.RenameConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.AIMessageResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.AttachDocumentsResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.DocumentQaConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.GenerationConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationResponse;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversationDocument;
import com.enterprise.aiassistant.backend.ai.conversation.enums.ConversationStatus;
import com.enterprise.aiassistant.backend.ai.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.conversation.mapper.AIConversationMapper;

import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationDocumentResponse;

import com.enterprise.aiassistant.backend.ai.conversation.repository.AIConversationDocumentRepository;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIConversationRepository;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIMessageRepository;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIMessageSourceRepository;

import com.enterprise.aiassistant.backend.ai.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.usage.repository.AIUsageLogRepository;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.DocumentException;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.repository.DocumentVersionRepository;
import com.enterprise.aiassistant.backend.generated.entity.GenerationRun;
import com.enterprise.aiassistant.backend.generated.repository.GenerationRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.enterprise.aiassistant.backend.common.exception.business_exception.ConversationException;

import com.enterprise.aiassistant.backend.generated.dto.response.GeneratedContentResponse;
import com.enterprise.aiassistant.backend.generated.mapper.GeneratedMapper;
import com.enterprise.aiassistant.backend.generated.repository.GeneratedContentRepository;
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

    private final GenerationRunRepository generationRunRepository;

    private final AIUsageLogRepository usageLogRepository;

    private final AIConversationMapper aiConversationMapper;

    private final AIConversationHelper aiConversationHelper;

    private final GeneratedMapper generatedMapper;

    private final AIMessageService aiMessageService;


    @Override
    @Transactional
    public ConversationResponse createConversation(CreateConversationRequest request) {

        aiConversationHelper.validateCreateConversationRequest(request);

        AIConversation conversation = aiConversationMapper.toEntity(request);
        conversationRepository.save(conversation);

        return aiConversationMapper.toResponse(conversation);
    }

    @Override
    @Transactional
    public ConversationResponse renameConversation(Long conversationId, RenameConversationRequest request) {

        aiConversationHelper.validateRenameRequest(conversationId, request);

        AIConversation conversation = conversationRepository.findByIdAndStatus(conversationId, ConversationStatus.ACTIVE)
                .orElseThrow(() -> new AIConversationException(ErrorCode.CONVERSATION_NOT_FOUND));

        conversation.setTitle(request.getTitle());
        conversationRepository.save(conversation);

        return aiConversationMapper.toResponse(conversation);
    }

    @Override
    @Transactional
    public void softDeleteConversation(Long conversationId) {

        aiConversationHelper.validateConversationId(conversationId);

        AIConversation conversation = conversationRepository.findByIdAndStatus(conversationId, ConversationStatus.ACTIVE)
                .orElseThrow(() -> new AIConversationException(ErrorCode.CONVERSATION_NOT_FOUND));

        conversation.setStatus(ConversationStatus.DELETED);
        conversation.setDeletedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    @Override
    @Transactional
    public void hardDeleteConversation(Long conversationId) {

        aiConversationHelper.validateConversationId(conversationId);

        // Hard delete must reach conversations already soft-deleted too, so no status filter here.
        AIConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AIConversationException(ErrorCode.CONVERSATION_NOT_FOUND));

        messageSourceRepository.deleteByAiMessage_ConversationId(conversationId);
        messageRepository.deleteByConversationId(conversationId);
        conversationDocumentRepository.deleteByConversationId(conversationId);
        generationRunRepository.deleteByAiConversationId(conversationId);
        generatedContentRepository.deleteByAiConversationId(conversationId);
        usageLogRepository.deleteByAiConversationId(conversationId);
        conversationRepository.delete(conversation);
    }

    @Override
    @Transactional
    public AttachDocumentsResponse attachDocuments(Long conversationId, AttachDocumentsRequest request) {

        // Validate request data
        aiConversationHelper.validateAttachRequest(conversationId, request);

        // Find active conversation
        AIConversation conversation = conversationRepository.findByIdAndStatus(conversationId, ConversationStatus.ACTIVE)
                .orElseThrow(() -> new AIConversationException(ErrorCode.CONVERSATION_NOT_FOUND));

        // Remove duplicate document IDs
        List<Long> documentVersionIds = request.getDocumentVersionIds().stream().distinct().toList();

        // Load document versions
        List<DocumentVersion> versions = documentVersionRepository.findAllById(documentVersionIds);

        // Ensure all requested documents exist
        if (versions.size() != documentVersionIds.size()) {
            throw new DocumentException(ErrorCode.DOCUMENT_VERSION_NOT_FOUND);
        }

        // Get already attached documents
        List<Long> alreadyAttachedIds =
                conversationDocumentRepository.findDocumentVersionIdsByConversationId(conversationId);

        // Filter out documents that are already attached
        List<DocumentVersion> newVersions =
                aiConversationHelper.filterNewVersions(versions, alreadyAttachedIds);

        // Create conversation-document mappings
        List<AIConversationDocument> newLinks = newVersions.stream()
                .map(version -> aiConversationMapper.toConversationDocument(conversation, version))
                .toList();

        // Save new document attachments
        conversationDocumentRepository.saveAll(newLinks);

        // Load all attached documents
        List<AIConversationDocument> allLinks =
                conversationDocumentRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);

        // Build response
        return aiConversationMapper.toAttachDocumentsResponse(allLinks);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponse> getConversations(
            ConversationFilterRequest filter,
            Pageable pageable
    ) {
        ConversationStatus status = filter.getStatus() != null ? filter.getStatus() : ConversationStatus.ACTIVE;

        return conversationRepository.filterConversations(filter.getConversationType(), status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentQaConversationDetailResponse getDocumentQaConversationDetail(
            Long conversationId,
            int recentMessagesLimit
    ) {

        aiConversationHelper.validateConversationId(conversationId);
        aiConversationHelper.validateRecentMessagesLimit(recentMessagesLimit);

        AIConversation conversation = conversationRepository.findByIdAndStatus(conversationId, ConversationStatus.ACTIVE)
                .orElseThrow(() -> new AIConversationException(ErrorCode.CONVERSATION_NOT_FOUND));

        List<ConversationDocumentResponse> attachedDocuments = getConversationDocuments(conversationId);

        Slice<AIMessageResponse> recentMessages = aiMessageService.getMessages(
                conversationId,
                PageRequest.of(0, recentMessagesLimit)
        );

        return aiConversationMapper.toDocumentQaDetailResponse(
                conversation,
                attachedDocuments,
                recentMessages.getContent(),
                recentMessages.hasNext()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public GenerationConversationDetailResponse getGenerationConversationDetail(Long conversationId) {

        aiConversationHelper.validateConversationId(conversationId);

        AIConversation conversation = conversationRepository.findByIdAndStatus(conversationId, ConversationStatus.ACTIVE)
                .orElseThrow(() -> new AIConversationException(ErrorCode.CONVERSATION_NOT_FOUND));

        aiConversationHelper.validateGenerationConversationType(conversation.getConversationType());

        GenerationRun generationRun = generationRunRepository
                .findFirstByAiConversationIdOrderByCreatedAtDesc(conversationId)
                .orElseThrow(() -> new AIConversationException(ErrorCode.GENERATION_RUN_NOT_FOUND));

        List<ConversationDocumentResponse> attachedDocuments =
                conversation.getConversationType() == ConversationType.EMAIL_GENERATION
                        ? null
                        : getConversationDocuments(conversationId);

        return aiConversationMapper.toGenerationDetailResponse(conversation, generationRun, attachedDocuments);
    }

    @Override
    public List<ConversationDocumentResponse> getConversationDocuments(Long conversationId) {

        getActiveConversationOrThrow(conversationId);

        return conversationDocumentRepository
                .findByAiConversationIdWithDocument(conversationId)
                .stream()
                .map(aiConversationMapper::toConversationDocumentResponse)
                .toList();
    }

    @Override
    @Transactional
    public void removeDocument(Long conversationId, Long documentVersionId) {

        getActiveConversationOrThrow(conversationId);

        AIConversationDocument conversationDocument = conversationDocumentRepository
                .findByConversationIdAndDocumentVersionId(conversationId, documentVersionId)
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

    private void getActiveConversationOrThrow(Long conversationId) {
        conversationRepository.findByIdAndStatus(conversationId, ConversationStatus.ACTIVE)
                .orElseThrow(() -> new ConversationException(ErrorCode.CONVERSATION_NOT_FOUND));

    }
}
