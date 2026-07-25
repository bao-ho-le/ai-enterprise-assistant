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
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AIConversationServiceImpl implements AIConversationService {

    private final AIConversationRepository conversationRepository;

    private final AIConversationDocumentRepository conversationDocumentRepository;

    private final ConversationMapper conversationMapper;

    private final ConversationHelper conversationHelper;

    @Override
    @Transactional
    public ConversationResponse createConversation(CreateConversationRequest request) {

        AIConversation conversation = conversationMapper.toEntity(request);
        conversationRepository.save(conversation);

        return conversationMapper.toResponse(conversation);
    }

    @Override
    @Transactional
    public ConversationResponse updateConversation(Long id, UpdateConversationRequest request) {

        AIConversation conversation = conversationHelper.getConversationOrThrow(id);

        conversation.setTitle(request.getTitle());
        conversationRepository.save(conversation);

        return conversationMapper.toResponse(conversation);
    }

    @Override
    @Transactional
    public void deleteConversation(Long id) {

        AIConversation conversation = conversationHelper.getConversationOrThrow(id);

        conversationRepository.deleteMessagesByConversationId(id);
        conversationRepository.deleteGeneratedContentByConversationId(id);
        conversationDocumentRepository.deleteByAiConversationId(id);
        conversationRepository.delete(conversation);
    }

    @Override
    @Transactional
    public ConversationDetailResponse attachDocuments(Long id, AttachDocumentsRequest request) {

        AIConversation conversation = conversationHelper.getConversationOrThrow(id);

        List<Long> versionIds = request.getDocumentVersionIds().stream().distinct().toList();
        List<DocumentVersion> versions = conversationHelper.getDocumentVersionsOrThrow(versionIds);

        List<Long> alreadyAttachedIds =
                conversationDocumentRepository.findDocumentVersionIdsByAiConversationId(id);

        List<DocumentVersion> newVersions =
                conversationHelper.filterNewVersions(versions, alreadyAttachedIds);

        List<AIConversationDocument> newLinks = newVersions.stream()
                .map(version -> conversationMapper.toConversationDocument(conversation, version))
                .toList();

        conversationDocumentRepository.saveAll(newLinks);

        List<AIConversationDocument> allLinks =
                conversationDocumentRepository.findByAiConversationIdWithDocument(id);

        return conversationMapper.toDetailResponse(conversation, allLinks);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationDocumentResponse> getConversationDocuments(Long conversationId) {

                conversationHelper.getConversationOrThrow(conversationId);

        return conversationDocumentRepository
                .findByAiConversationIdWithDocument(conversationId)
                .stream()
                .map(conversationMapper::toConversationDocumentResponse)
                .toList();
    }

    @Override
    @Transactional
    public void removeDocument(Long conversationId, Long documentVersionId) {

        conversationHelper.getConversationOrThrow(conversationId);

        AIConversationDocument conversationDocument = conversationHelper.validateAttachedDocument(
                        conversationId,
                        documentVersionId);

        conversationDocumentRepository.delete(conversationDocument);
    }
}
