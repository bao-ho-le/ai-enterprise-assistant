package com.enterprise.aiassistant.backend.ai.conversation.service;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.AttachDocumentsRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.ConversationFilterRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.CreateConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.request.RenameConversationRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.MessageResponse;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversationDocument;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIMessage;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIMessageSource;
import com.enterprise.aiassistant.backend.ai.conversation.enums.ConversationStatus;
import com.enterprise.aiassistant.backend.ai.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.conversation.helper.ConversationHelper;
import com.enterprise.aiassistant.backend.ai.conversation.mapper.AIConversationMapper;
import com.enterprise.aiassistant.backend.ai.conversation.mapper.ConversationMapper;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIConversationDocumentRepository;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIConversationRepository;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIMessageRepository;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIMessageSourceRepository;
import com.enterprise.aiassistant.backend.ai.usage.repository.AIUsageLogRepository;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.DocumentException;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.repository.DocumentVersionRepository;
import com.enterprise.aiassistant.backend.generated.entity.GeneratedContent;
import com.enterprise.aiassistant.backend.generated.repository.GeneratedContentRepository;
import com.enterprise.aiassistant.backend.generated.repository.GenerationRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private final ConversationMapper conversationMapper;

    private final AIConversationMapper aiConversationMapper;

    private final ConversationHelper conversationHelper;

    private final AIConversationHelper aiConversationHelper;

    @Override
    @Transactional
    public ConversationResponse createConversation(CreateConversationRequest request) {

        conversationHelper.validateCreateRequest(request);

        AIConversation conversation = conversationMapper.toEntity(request);
        conversationRepository.save(conversation);

        return conversationMapper.toResponse(conversation);
    }

    @Override
    @Transactional
    public ConversationResponse renameConversation(Long conversationId, RenameConversationRequest request) {

        conversationHelper.validateRenameRequest(conversationId, request);

        AIConversation conversation = conversationRepository.findByIdAndStatus(conversationId, ConversationStatus.ACTIVE)
                .orElseThrow(() -> new AIConversationException(ErrorCode.CONVERSATION_NOT_FOUND));

        conversation.setTitle(request.getTitle());
        conversationRepository.save(conversation);

        return conversationMapper.toResponse(conversation);
    }

    @Override
    @Transactional
    public void softDeleteConversation(Long conversationId) {

        conversationHelper.validateConversationId(conversationId);

        AIConversation conversation = conversationRepository.findByIdAndStatus(conversationId, ConversationStatus.ACTIVE)
                .orElseThrow(() -> new AIConversationException(ErrorCode.CONVERSATION_NOT_FOUND));

        conversation.setStatus(ConversationStatus.DELETED);
        conversation.setDeletedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    @Override
    @Transactional
    public void hardDeleteConversation(Long conversationId) {

        conversationHelper.validateConversationId(conversationId);

        // Hard delete must reach conversations already soft-deleted too, so no status filter here.
        AIConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AIConversationException(ErrorCode.CONVERSATION_NOT_FOUND));

        // All relations are unidirectional (child owns the FK, no cascade) - delete dependents
        // explicitly in FK order: message sources before messages; generation runs before generated
        // content (generation_runs.generated_content_id references generated_content).
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
    public ConversationDetailResponse attachDocuments(Long conversationId, AttachDocumentsRequest request) {

        conversationHelper.validateAttachRequest(conversationId, request);

        AIConversation conversation = conversationRepository.findByIdAndStatus(conversationId, ConversationStatus.ACTIVE)
                .orElseThrow(() -> new AIConversationException(ErrorCode.CONVERSATION_NOT_FOUND));

        List<Long> documentVersionIds = request.getDocumentVersionIds().stream().distinct().toList();
        List<DocumentVersion> versions = documentVersionRepository.findAllById(documentVersionIds);

        if (versions.size() != documentVersionIds.size()) {
            throw new DocumentException(ErrorCode.DOCUMENT_VERSION_NOT_FOUND);
        }

        List<Long> alreadyAttachedIds =
                conversationDocumentRepository.findDocumentVersionIdsByConversationId(conversationId);

        List<DocumentVersion> newVersions =
                conversationHelper.filterNewVersions(versions, alreadyAttachedIds);

        List<AIConversationDocument> newLinks = newVersions.stream()
                .map(version -> conversationMapper.toConversationDocument(conversation, version))
                .toList();

        conversationDocumentRepository.saveAll(newLinks);

        List<AIConversationDocument> allLinks =
                conversationDocumentRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);

        return aiConversationMapper.toDetailResponse(
                conversation,
                allLinks,
                Collections.emptyList(),
                0L,
                Collections.emptyMap(),
                Collections.emptyList(),
                null,
                null
        );
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
    public ConversationDetailResponse getConversationDetail(
            Long conversationId,
            int recentMessagesLimit
    ) {

        aiConversationHelper.validateConversationId(conversationId);
        aiConversationHelper.validateRecentMessagesLimit(recentMessagesLimit);

        AIConversation conversation = conversationRepository.findByIdAndStatus(conversationId, ConversationStatus.ACTIVE)
                .orElseThrow(() -> new AIConversationException(ErrorCode.CONVERSATION_NOT_FOUND));

        List<AIConversationDocument> documents =
                conversationDocumentRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);

        // Lấy N message gần nhất (giảm dần), rồi đảo lại thành thứ tự thời gian tăng dần để hiển thị.
        Page<AIMessage> recentPage = messageRepository.findByConversationIdOrderByCreatedAtDesc(
                conversationId,
                PageRequest.of(0, recentMessagesLimit)
        );

        List<AIMessage> recentMessagesAsc = recentPage.getContent().stream()
                .sorted(Comparator.comparing(AIMessage::getCreatedAt))
                .toList();

        List<Long> messageIds = recentMessagesAsc.stream().map(AIMessage::getId).toList();

        Map<Long, List<AIMessageSource>> sourcesByMessageId = messageIds.isEmpty()
                ? Collections.emptyMap()
                : messageSourceRepository.findByMessageIdIn(messageIds).stream()
                        .collect(Collectors.groupingBy(source -> source.getAiMessage().getId()));

        List<GeneratedContent> generatedContents =
                generatedContentRepository.findByAiConversationIdOrderByCreatedAtDesc(conversationId);

        Long totalTokens = usageLogRepository.sumTotalTokensByConversationId(conversationId);
        var estimatedCost = usageLogRepository.sumEstimatedCostByConversationId(conversationId);

        return aiConversationMapper.toDetailResponse(
                conversation,
                documents,
                recentMessagesAsc,
                recentPage.getTotalElements(),
                sourcesByMessageId,
                generatedContents,
                totalTokens,
                estimatedCost
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getConversationMessages(Long conversationId, Pageable pageable) {

        aiConversationHelper.validateConversationId(conversationId);

        if (!conversationRepository.existsByIdAndStatus(conversationId, ConversationStatus.ACTIVE)) {
            throw new AIConversationException(ErrorCode.CONVERSATION_NOT_FOUND);
        }

        Pageable effectivePageable = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").ascending());

        Page<AIMessage> page =
                messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, effectivePageable);

        List<Long> messageIds = page.getContent().stream().map(AIMessage::getId).toList();

        Map<Long, List<AIMessageSource>> sourcesByMessageId = messageIds.isEmpty()
                ? Collections.emptyMap()
                : messageSourceRepository.findByMessageIdIn(messageIds).stream()
                        .collect(Collectors.groupingBy(source -> source.getAiMessage().getId()));

        return page.map(m -> aiConversationMapper.toMessageResponse(m, sourcesByMessageId));
    }
}
