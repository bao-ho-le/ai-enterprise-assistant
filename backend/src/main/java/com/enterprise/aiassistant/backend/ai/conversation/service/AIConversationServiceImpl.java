package com.enterprise.aiassistant.backend.ai.conversation.service;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.AttachDocumentsRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.CreateConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.RenameConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationResponse;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversationDocument;
import com.enterprise.aiassistant.backend.ai.conversation.helper.ConversationHelper;
import com.enterprise.aiassistant.backend.ai.conversation.mapper.ConversationMapper;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIConversationDocumentRepository;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIConversationRepository;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ConversationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.DocumentException;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AIConversationServiceImpl implements AIConversationService {

    private final AIConversationRepository conversationRepository;

    private final AIConversationDocumentRepository conversationDocumentRepository;

    private final DocumentVersionRepository documentVersionRepository;

    private final ConversationMapper conversationMapper;

    private final ConversationHelper conversationHelper;

    @Override
    @Transactional
    public ConversationResponse createConversation(CreateConversationRequest request) {

        validateCreateRequest(request);

        AIConversation conversation = conversationMapper.toEntity(request);
        conversationRepository.save(conversation);

        return conversationMapper.toResponse(conversation);
    }

    @Override
    @Transactional
    public ConversationResponse renameConversation(Long conversationId, RenameConversationRequest request) {

        validateRenameRequest(conversationId, request);

        AIConversation conversation = conversationRepository.findByIdAndDeletedFalse(conversationId)
                .orElseThrow(() -> new ConversationException(ErrorCode.CONVERSATION_NOT_FOUND));

        conversation.setTitle(request.getTitle());
        conversationRepository.save(conversation);

        return conversationMapper.toResponse(conversation);
    }

    @Override
    @Transactional
    public void softDeleteConversation(Long conversationId) {

        validateConversationId(conversationId);

        AIConversation conversation = conversationRepository.findByIdAndDeletedFalse(conversationId)
                .orElseThrow(() -> new ConversationException(ErrorCode.CONVERSATION_NOT_FOUND));

        conversation.setDeleted(true);
        conversation.setDeletedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    @Override
    @Transactional
    public void hardDeleteConversation(Long conversationId) {

        validateConversationId(conversationId);

        // Hard delete must reach conversations already soft-deleted too, so no deletedFalse filter here.
        AIConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationException(ErrorCode.CONVERSATION_NOT_FOUND));

        // Cascade + orphanRemoval on AIConversation's relations handles messages/documents/usageLogs/generatedContents.
        conversationRepository.delete(conversation);
    }

    @Override
    @Transactional
    public ConversationDetailResponse attachDocuments(Long conversationId, AttachDocumentsRequest request) {

        validateAttachRequest(conversationId, request);

        AIConversation conversation = conversationRepository.findByIdAndDeletedFalse(conversationId)
                .orElseThrow(() -> new ConversationException(ErrorCode.CONVERSATION_NOT_FOUND));

        List<Long> documentVersionIds = request.getDocumentVersionIds().stream().distinct().toList();
        List<DocumentVersion> versions = documentVersionRepository.findAllById(documentVersionIds);

        if (versions.size() != documentVersionIds.size()) {
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

    private void validateConversationId(Long conversationId) {
        if (conversationId == null || conversationId <= 0) {
            throw new ConversationException(ErrorCode.CONVERSATION_NOT_FOUND);
        }
    }

    private void validateCreateRequest(CreateConversationRequest request) {
        if (request == null || request.getTitle() == null || request.getTitle().isBlank()
                || request.getConversationType() == null) {
            throw new BusinessException(ErrorCode.REQUEST_REQUIRED);
        }
    }

    private void validateRenameRequest(Long conversationId, RenameConversationRequest request) {
        validateConversationId(conversationId);
        if (request == null || request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BusinessException(ErrorCode.REQUEST_REQUIRED);
        }
    }

    private void validateAttachRequest(Long conversationId, AttachDocumentsRequest request) {
        validateConversationId(conversationId);
        if (request == null || request.getDocumentVersionIds() == null || request.getDocumentVersionIds().isEmpty()) {
            throw new BusinessException(ErrorCode.REQUEST_REQUIRED);
        }
    }
}
