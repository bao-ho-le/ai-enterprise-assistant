package com.enterprise.aiassistant.backend.ai.chat.message.service;

import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.analytics.usage.listener.AIUsageContext;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.chat.conversation.enums.ConversationStatus;
import com.enterprise.aiassistant.backend.ai.chat.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.chat.conversation.repository.AIConversationRepository;
import com.enterprise.aiassistant.backend.ai.chat.handler.service.ConversationSummaryChatService;
import com.enterprise.aiassistant.backend.ai.chat.handler.service.GeneralChatService;
import com.enterprise.aiassistant.backend.ai.chat.handler.service.SummaryChatService;
import com.enterprise.aiassistant.backend.ai.chat.intent.enums.Intent;
import com.enterprise.aiassistant.backend.ai.chat.intent.service.IntentClassifier;
import com.enterprise.aiassistant.backend.ai.chat.memory.service.ConversationMemoryService;
import com.enterprise.aiassistant.backend.ai.chat.message.dto.request.SendMessageRequest;
import com.enterprise.aiassistant.backend.ai.chat.message.dto.response.MessageDetailResponse;
import com.enterprise.aiassistant.backend.ai.chat.message.dto.response.MessagePageResponse;
import com.enterprise.aiassistant.backend.ai.chat.message.dto.response.MessageResponse;
import com.enterprise.aiassistant.backend.ai.chat.message.entity.AIMessage;
import com.enterprise.aiassistant.backend.ai.chat.message.entity.AIMessageSource;
import com.enterprise.aiassistant.backend.ai.chat.message.enums.AIMessageRole;
import com.enterprise.aiassistant.backend.ai.chat.message.helper.AIMessageHelper;
import com.enterprise.aiassistant.backend.ai.chat.message.mapper.AIMessageMapper;
import com.enterprise.aiassistant.backend.ai.chat.message.repository.AIMessageRepository;
import com.enterprise.aiassistant.backend.ai.chat.message.repository.AIMessageSourceRepository;
import com.enterprise.aiassistant.backend.ai.knowledge.qa.service.DocumentQAService;
import com.enterprise.aiassistant.backend.ai.knowledge.qa.service.QuestionRewriter;
import com.enterprise.aiassistant.backend.auth.service.CurrentUserService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthorizationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.ConversationException;
import com.enterprise.aiassistant.backend.document.entity.DocumentChunk;
import com.enterprise.aiassistant.backend.document.repository.DocumentChunkRepository;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIMessageServiceImplTest {

    @Mock
    private AIConversationRepository conversationRepository;

    @Mock
    private AIConversationHelper aiConversationHelper;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AIMessageRepository messageRepository;

    @Mock
    private AIMessageSourceRepository messageSourceRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private AIMessageMapper messageMapper;

    @Mock
    private AIMessageHelper messageHelper;

    @Mock
    private DocumentQAService documentQAService;

    @Mock
    private QuestionRewriter questionRewriter;

    @Mock
    private SummaryChatService summaryChatService;

    @Mock
    private GeneralChatService generalChatService;

    @Mock
    private ConversationSummaryChatService conversationSummaryChatService;

    @Mock
    private IntentClassifier intentClassifier;

    @Mock
    private ConversationMemoryService conversationMemoryService;

    @InjectMocks
    private AIMessageServiceImpl service;

    private static final String CONTENT = "Nó có mấy loại?";

    private final AIConversation conversation = AIConversation.builder()
            .id(1L)
            .conversationType(ConversationType.DOCUMENT_QA)
            .build();

    private final AIMessage userMessage = AIMessage.builder().role(AIMessageRole.USER).content(CONTENT).build();
    private final AIMessage savedUserMessage = AIMessage.builder().id(10L).role(AIMessageRole.USER).content(CONTENT).build();
    private final AIMessage assistantMessage = AIMessage.builder().id(11L).role(AIMessageRole.ASSISTANT).content("Có 2 loại").build();

    private final MessageResponse response = MessageResponse.builder().build();

    private SendMessageRequest request;

    @BeforeEach
    void setUp() {
        request = new SendMessageRequest();
        request.setContent(CONTENT);
    }

    // ===================== sendMessage: 4 intent đi đúng 4 nhánh =====================

    @Test
    void sendMessage_documentQaIntent_rewritesQuestionBeforeAnswering() {
        stubUntilIntent(Intent.DOCUMENT_QA);
        when(questionRewriter.rewrite(conversation, CONTENT)).thenReturn("Hợp đồng lao động có mấy loại?");
        // Stub chỉ khớp với câu đã viết lại: nếu service gửi câu gốc thì mock trả null và test fail
        when(documentQAService.answer(conversation, "Hợp đồng lao động có mấy loại?")).thenReturn(assistantMessage);
        when(messageMapper.toMessageResponse(savedUserMessage, assistantMessage)).thenReturn(response);

        assertThat(service.sendMessage(1L, request)).isSameAs(response);

        verify(conversationMemoryService).appendTurn(conversation, CONTENT, "Có 2 loại");
        verifyNoInteractions(summaryChatService, generalChatService, conversationSummaryChatService);
    }

    @Test
    void sendMessage_summaryIntent_savesSummaryAsAssistantMessage() {
        stubUntilIntent(Intent.SUMMARY);
        when(summaryChatService.summarize(conversation, CONTENT)).thenReturn("Có 2 loại");
        stubSaveAssistantMessage();

        assertThat(service.sendMessage(1L, request)).isSameAs(response);

        verify(conversationMemoryService).appendTurn(conversation, CONTENT, "Có 2 loại");
        verifyNoInteractions(documentQAService, questionRewriter);
    }

    @Test
    void sendMessage_generalChatIntent_answersWithoutDocuments() {
        stubUntilIntent(Intent.GENERAL_CHAT);
        when(generalChatService.answer(CONTENT)).thenReturn("Có 2 loại");
        stubSaveAssistantMessage();

        assertThat(service.sendMessage(1L, request)).isSameAs(response);

        verifyNoInteractions(documentQAService, questionRewriter, summaryChatService);
    }

    @Test
    void sendMessage_conversationSummaryIntent_summarizesConversation() {
        stubUntilIntent(Intent.CONVERSATION_SUMMARY);
        when(conversationSummaryChatService.summarize(conversation, CONTENT)).thenReturn("Có 2 loại");
        stubSaveAssistantMessage();

        assertThat(service.sendMessage(1L, request)).isSameAs(response);

        verifyNoInteractions(documentQAService, questionRewriter, generalChatService);
    }

    // ===================== sendMessage: permission theo intent =====================

    // Mỗi dòng là một cặp (intent, permission bắt buộc). JUnit tự đổi chuỗi sang enum.
    @ParameterizedTest
    @CsvSource({
            "DOCUMENT_QA,          AI_DOCUMENT_QA",
            "SUMMARY,              AI_DOCUMENT_SUMMARY",
            "GENERAL_CHAT,         AI_CHAT",
            "CONVERSATION_SUMMARY, AI_CHAT"
    })
    void sendMessage_missingIntentPermission_keepsUserMessageButSkipsAi(Intent intent, Permission permission) {
        stubUntilIntent(intent);
        // Stub chỉ ném lỗi với đúng permission của dòng này: service kiểm tra sai permission thì không có lỗi và test fail
        doThrow(new AuthorizationException(ErrorCode.PERMISSION_DENIED))
                .when(currentUserService).requirePermission(permission);

        assertThatThrownBy(() -> service.sendMessage(1L, request))
                .isInstanceOf(AuthorizationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PERMISSION_DENIED);

        // Tin nhắn user đã lưu trước khi phân loại intent; không nhánh AI nào được chạy
        verify(messageRepository).save(userMessage);
        verifyNoInteractions(documentQAService, questionRewriter, summaryChatService,
                generalChatService, conversationSummaryChatService, conversationMemoryService);
    }

    // ===================== sendMessage: các trường hợp khác =====================

    // DocumentQAService trả null khi LLM lỗi: không được ghi lượt chat dở dang vào memory
    @Test
    void sendMessage_noAssistantAnswer_doesNotAppendMemory() {
        stubUntilIntent(Intent.DOCUMENT_QA);
        when(questionRewriter.rewrite(conversation, CONTENT)).thenReturn(CONTENT);
        when(documentQAService.answer(conversation, CONTENT)).thenReturn(null);
        when(messageMapper.toMessageResponse(savedUserMessage, null)).thenReturn(response);

        assertThat(service.sendMessage(1L, request)).isSameAs(response);

        verify(conversationMemoryService, never()).appendTurn(any(), any(), any());
    }

    @Test
    void sendMessage_notChatConversation_throwsBeforeSavingMessage() {
        conversation.setConversationType(ConversationType.EMAIL_GENERATION);
        stubActiveConversation();
        doThrow(new ConversationException(ErrorCode.CONVERSATION_TYPE_NOT_CHAT))
                .when(messageHelper).validateChatConversationType(ConversationType.EMAIL_GENERATION);

        assertThatThrownBy(() -> service.sendMessage(1L, request))
                .isInstanceOf(ConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONVERSATION_TYPE_NOT_CHAT);

        verify(messageRepository, never()).save(any());
        verifyNoInteractions(intentClassifier);
    }

    // AIUsageListener cần biết lượt gọi AI thuộc conversation nào. thenAnswer cho phép chạy code
    // NGAY LÚC mock được gọi, nên đọc được context đang mở ở giữa quá trình gửi tin nhắn.
    @Test
    void sendMessage_duringAiCalls_opensUsageContextForConversation() {
        AtomicReference<AIUsageContext> contextDuringCall = new AtomicReference<>();

        stubActiveConversation();
        when(messageMapper.toMessage(conversation, AIMessageRole.USER, CONTENT)).thenReturn(userMessage);
        when(messageRepository.save(userMessage)).thenReturn(savedUserMessage);
        when(intentClassifier.classify(CONTENT)).thenAnswer(invocation -> {
            contextDuringCall.set(AIUsageContext.current().orElse(null));
            return Intent.GENERAL_CHAT;
        });
        when(generalChatService.answer(CONTENT)).thenReturn("Có 2 loại");
        stubSaveAssistantMessage();

        service.sendMessage(1L, request);

        assertThat(contextDuringCall.get()).isNotNull();
        assertThat(contextDuringCall.get().getConversationType()).isEqualTo(ConversationType.DOCUMENT_QA);
        assertThat(contextDuringCall.get().getConversationId()).isEqualTo(1L);
        assertThat(contextDuringCall.get().getGenerationId()).isNull();
        // Ra khỏi sendMessage thì context phải được dọn, không rò sang request sau trên cùng thread
        assertThat(AIUsageContext.current()).isEmpty();
    }

    // ===================== getMessages =====================

    @Test
    void getMessages_noBeforeId_startsFromNewestMessage() {
        Slice<AIMessage> slice = new SliceImpl<>(List.of(savedUserMessage));
        MessagePageResponse pageResponse = MessagePageResponse.builder().build();

        stubActiveConversation();
        when(messageRepository.findByConversationIdAndIdLessThanOrderByIdDesc(1L, Long.MAX_VALUE, PageRequest.of(0, 20)))
                .thenReturn(slice);
        when(messageMapper.toMessagePageResponse(slice)).thenReturn(pageResponse);

        assertThat(service.getMessages(1L, null, 20)).isSameAs(pageResponse);
    }

    @Test
    void getMessages_givenBeforeId_loadsOlderMessages() {
        Slice<AIMessage> slice = new SliceImpl<>(List.of(savedUserMessage));
        MessagePageResponse pageResponse = MessagePageResponse.builder().build();

        stubActiveConversation();
        when(messageRepository.findByConversationIdAndIdLessThanOrderByIdDesc(1L, 50L, PageRequest.of(0, 20)))
                .thenReturn(slice);
        when(messageMapper.toMessagePageResponse(slice)).thenReturn(pageResponse);

        assertThat(service.getMessages(1L, 50L, 20)).isSameAs(pageResponse);
    }

    // ===================== getMessageEvidence =====================

    @Test
    void getMessageEvidence_duplicateChunkIds_loadsEachChunkOnce() {
        AIMessageSource source1 = AIMessageSource.builder().chunkId(7L).build();
        AIMessageSource source2 = AIMessageSource.builder().chunkId(7L).build();
        AIMessageSource source3 = AIMessageSource.builder().chunkId(8L).build();
        DocumentChunk chunk7 = DocumentChunk.builder().id(7L).build();
        DocumentChunk chunk8 = DocumentChunk.builder().id(8L).build();
        MessageDetailResponse detail = MessageDetailResponse.builder().build();

        stubActiveConversation();
        when(messageRepository.findByIdAndConversationId(11L, 1L)).thenReturn(Optional.of(assistantMessage));
        when(messageSourceRepository.findByAiMessageIdOrderByIdAsc(11L)).thenReturn(List.of(source1, source2, source3));
        // Chunk 7 xuất hiện 2 lần nhưng chỉ được query 1 lần
        when(documentChunkRepository.findAllById(List.of(7L, 8L))).thenReturn(List.of(chunk7, chunk8));
        when(messageMapper.toDetailResponse(assistantMessage, List.of(source1, source2, source3), Map.of(7L, chunk7, 8L, chunk8)))
                .thenReturn(detail);

        assertThat(service.getMessageEvidence(1L, 11L)).isSameAs(detail);
    }

    @Test
    void getMessageEvidence_noSources_skipsChunkQuery() {
        MessageDetailResponse detail = MessageDetailResponse.builder().build();

        stubActiveConversation();
        when(messageRepository.findByIdAndConversationId(11L, 1L)).thenReturn(Optional.of(assistantMessage));
        when(messageMapper.toDetailResponse(assistantMessage, List.of(), Map.of())).thenReturn(detail);

        assertThat(service.getMessageEvidence(1L, 11L)).isSameAs(detail);

        verifyNoInteractions(documentChunkRepository);
    }

    @Test
    void getMessageEvidence_messageNotInConversation_throwsMessageNotFound() {
        stubActiveConversation();
        when(messageRepository.findByIdAndConversationId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMessageEvidence(1L, 99L))
                .isInstanceOf(ConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MESSAGE_NOT_FOUND);

        verifyNoInteractions(messageSourceRepository);
    }

    // ===================== Helper =====================

    private void stubActiveConversation() {
        when(conversationRepository.findByIdAndStatus(1L, ConversationStatus.ACTIVE)).thenReturn(Optional.of(conversation));
    }

    // Các bước chung của sendMessage tới lúc phân loại intent: tìm conversation, lưu tin nhắn user
    private void stubUntilIntent(Intent intent) {
        stubActiveConversation();
        when(messageMapper.toMessage(conversation, AIMessageRole.USER, CONTENT)).thenReturn(userMessage);
        when(messageRepository.save(userMessage)).thenReturn(savedUserMessage);
        when(intentClassifier.classify(CONTENT)).thenReturn(intent);
    }

    // Nhánh SUMMARY / GENERAL_CHAT / CONVERSATION_SUMMARY tự lưu câu trả lời thành tin nhắn assistant
    private void stubSaveAssistantMessage() {
        when(messageMapper.toMessage(conversation, AIMessageRole.ASSISTANT, "Có 2 loại")).thenReturn(assistantMessage);
        when(messageRepository.save(assistantMessage)).thenReturn(assistantMessage);
        when(messageMapper.toMessageResponse(savedUserMessage, assistantMessage)).thenReturn(response);
    }
}
