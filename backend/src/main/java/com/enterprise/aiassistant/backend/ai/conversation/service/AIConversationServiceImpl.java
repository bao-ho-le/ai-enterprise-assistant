package com.enterprise.aiassistant.backend.ai.conversation.service;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.ConversationFilterRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationResponse;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.MessageResponse;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversationDocument;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIMessage;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIMessageSource;
import com.enterprise.aiassistant.backend.ai.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.conversation.mapper.AIConversationMapper;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIConversationDocumentRepository;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIConversationRepository;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIMessageRepository;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIMessageSourceRepository;
import com.enterprise.aiassistant.backend.ai.usage.repository.AIUsageLogRepository;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import com.enterprise.aiassistant.backend.generated.entity.GeneratedContent;
import com.enterprise.aiassistant.backend.generated.repository.GeneratedContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.enterprise.aiassistant.backend.common.exception.ErrorCode.CONVERSATION_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AIConversationServiceImpl implements AIConversationService {

    private final AIConversationRepository conversationRepository;

    private final AIConversationDocumentRepository conversationDocumentRepository;

    private final AIMessageRepository messageRepository;

    private final AIMessageSourceRepository messageSourceRepository;

    private final GeneratedContentRepository generatedContentRepository;

    private final AIUsageLogRepository usageLogRepository;

    private final AIConversationMapper conversationMapper;

    private final AIConversationHelper conversationHelper;

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponse> getConversations(
            ConversationFilterRequest filter,
            Pageable pageable
    ) {
        conversationHelper.validateFilter(filter);
        conversationHelper.validatePageable(pageable);

        return conversationRepository.filterConversations(filter, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationDetailResponse getConversationDetail(
            Long conversationId,
            int recentMessagesLimit
    ) {

        conversationHelper.validateConversationId(conversationId);
        conversationHelper.validateRecentMessagesLimit(recentMessagesLimit);

        AIConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AIConversationException(CONVERSATION_NOT_FOUND));

        List<AIConversationDocument> documents =
                conversationDocumentRepository.findByConversationIdOrderByAttachedAtAsc(conversationId);

        // Lấy N message gần nhất (giảm dần), rồi đảo lại thành thứ tự thời gian tăng dần để hiển thị.
        Page<AIMessage> recentPage = messageRepository.findByConversationIdOrderByCreatedAtDesc(
                conversationId,
                PageRequest.of(0, Math.max(recentMessagesLimit, 1))
        );

        List<AIMessage> recentMessagesAsc = recentPage.getContent().stream()
                .sorted(Comparator.comparing(AIMessage::getCreatedAt))
                .toList();

        List<Long> messageIds = recentMessagesAsc.stream().map(AIMessage::getId).toList();

        Map<Long, List<AIMessageSource>> sourcesByMessageId = messageIds.isEmpty()
                ? Collections.emptyMap()
                : messageSourceRepository.findByMessageIdIn(messageIds).stream()
                        .collect(Collectors.groupingBy(source -> source.getMessage().getId()));

        List<GeneratedContent> generatedContents =
                generatedContentRepository.findByAiConversationIdOrderByCreatedAtDesc(conversationId);

        Long totalTokens = usageLogRepository.sumTotalTokensByConversationId(conversationId);
        var estimatedCost = usageLogRepository.sumEstimatedCostByConversationId(conversationId);

        return conversationMapper.toDetailResponse(
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

        conversationHelper.validateConversationId(conversationId);
        conversationHelper.validatePageable(pageable);

        if (!conversationRepository.existsById(conversationId)) {
            throw new AIConversationException(CONVERSATION_NOT_FOUND);
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
                        .collect(Collectors.groupingBy(source -> source.getMessage().getId()));

        return page.map(m -> conversationMapper.toMessageResponse(m, sourcesByMessageId));
    }
}
