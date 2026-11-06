package com.enterprise.aiassistant.backend.ai.knowledge.generation.service;

import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.response.ConversationDocumentResponse;
import com.enterprise.aiassistant.backend.ai.chat.conversation.dto.response.GenerationConversationDetailResponse;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversationDocument;
import com.enterprise.aiassistant.backend.ai.chat.conversation.enums.ConversationStatus;
import com.enterprise.aiassistant.backend.ai.chat.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.chat.conversation.mapper.AIConversationMapper;
import com.enterprise.aiassistant.backend.ai.chat.conversation.repository.AIConversationDocumentRepository;
import com.enterprise.aiassistant.backend.ai.chat.conversation.repository.AIConversationRepository;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.helper.LLMHelper;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.GenerationContext;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.request.TriggerGenerationRequest;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.response.TriggerGenerationResponse;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.entity.GeneratedContent;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.entity.Generation;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.enums.GeneratedDocumentType;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.enums.GeneratedType;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.enums.GenerationStatus;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.handler.GenerationHandler;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.helper.GenerationHelper;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.mapper.GeneratedMapper;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.mapper.GenerationMapper;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.repository.GeneratedContentRepository;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.repository.GenerationRepository;
import com.enterprise.aiassistant.backend.auth.service.CurrentUserService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthorizationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.LLMException;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerationServiceImplTest {

    @Mock
    private AIConversationRepository conversationRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AIConversationDocumentRepository conversationDocumentRepository;

    @Mock
    private GenerationRepository generationRepository;

    @Mock
    private GeneratedContentRepository generatedContentRepository;

    @Mock
    private AIConversationHelper aiConversationHelper;

    @Mock
    private AIConversationMapper aiConversationMapper;

    @Mock
    private GenerationHelper generationHelper;

    @Mock
    private GeneratedMapper generatedMapper;

    @Mock
    private GenerationMapper generationMapper;

    @Mock
    private ChatModel chatModel;

    @Mock
    private LLMHelper llmHelper;

    @Mock
    private GenerationHandler emailHandler;

    @Mock
    private GenerationHandler reportHandler;

    private GenerationServiceImpl service;

    private TriggerGenerationRequest request;

    private final AIConversation conversation = AIConversation.builder()
            .id(1L)
            .conversationType(ConversationType.EMAIL_GENERATION)
            .build();

    private final JsonNode inputData = JsonNodeFactory.instance.objectNode().put("purpose", "Xin nghỉ phép");

    private final GenerationContext context = GenerationContext.builder()
            .prompt("PROMPT")
            .title("Xin nghỉ phép")
            .generatedType(GeneratedType.EMAIL)
            .build();

    private final Generation generation = Generation.builder().id(10L).build();

    // @InjectMocks không tự tạo được List<GenerationHandler> (nó sẽ truyền null),
    // nên tự new service và truyền danh sách handler giả vào.
    @BeforeEach
    void setUp() {
        service = new GenerationServiceImpl(
                conversationRepository, currentUserService, conversationDocumentRepository, generationRepository,
                generatedContentRepository, aiConversationHelper, aiConversationMapper, generationHelper,
                generatedMapper, generationMapper, chatModel, llmHelper,
                List.of(reportHandler, emailHandler)
        );

        request = new TriggerGenerationRequest();
        request.setInputData(Map.of("purpose", "Xin nghỉ phép"));
    }

    // ===================== generate: thành công =====================

    @Test
    void generate_llmSucceeds_savesContentAndMarksCompleted() {
        GeneratedContent content = GeneratedContent.builder().id(20L).build();
        TriggerGenerationResponse response = TriggerGenerationResponse.builder().build();

        stubUntilLlmCall();
        when(llmHelper.call(any())).thenReturn(ChatResponse.builder().aiMessage(AiMessage.from("Nội dung AI")).build());
        when(generatedMapper.toCreateGeneratedContentObject(GeneratedDocumentType.EMAIL, "Xin nghỉ phép", "Nội dung AI"))
                .thenReturn(content);
        when(generatedContentRepository.save(content)).thenReturn(content);
        when(generationMapper.toTriggerGenerationResponse(generation, content)).thenReturn(response);

        assertThat(service.generate(1L, request)).isSameAs(response);

        assertThat(generation.getStatus()).isEqualTo(GenerationStatus.COMPLETED);
        assertThat(generation.getGeneratedContent()).isSameAs(content);
        // Lần 1: tạo mới, lần 2: chuyển RUNNING, lần 3: chuyển COMPLETED
        verify(generationRepository, times(3)).save(generation);
        // Conversation là email nên chỉ emailHandler được dùng
        verify(reportHandler, never()).handle(any(), any());
    }

    // ===================== generate: lỗi =====================

    @Test
    void generate_llmFails_marksFailedAndThrowsRunFailed() {
        LLMException llmError = new LLMException(ErrorCode.LLM_GENERATION_FAILED);

        stubUntilLlmCall();
        when(llmHelper.call(any())).thenThrow(llmError);

        assertThatThrownBy(() -> service.generate(1L, request))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GENERATION_RUN_FAILED)
                .hasCause(llmError);

        // Lần chạy lỗi vẫn phải được lưu lại (FAILED + lý do) để user thấy, không mất dấu.
        // Không dùng ArgumentCaptor được: 3 lần save cùng truyền một object generation, captor chỉ giữ
        // tham chiếu, nên cả 3 giá trị bắt được đều đang ở trạng thái cuối cùng (FAILED).
        assertThat(generation.getStatus()).isEqualTo(GenerationStatus.FAILED);
        assertThat(generation.getErrorMessage()).isEqualTo(llmError.getMessage());
        verify(generationRepository, times(3)).save(generation);
        verify(generatedContentRepository, never()).save(any());
    }

    // TODO (bạn tự viết): generate_contentSaveFails_marksGenerationFailed
    // LLM trả lời thành công, nhưng lúc lưu GeneratedContent thì DB lỗi:
    // generatedContentRepository.save(any()) ném new RuntimeException("Mất kết nối DB")
    // → service ném AIConversationException với ErrorCode.GENERATION_RUN_FAILED,
    //   generation chuyển FAILED và errorMessage = "Mất kết nối DB".
    // Gợi ý: kết hợp test thành công (stub LLM trả ChatResponse) với test LLM lỗi (phần assert).
    // Không cần stub generatedMapper: mock trả null, save(any()) vẫn khớp với null.
    @Test
    void generate_contentSaveFails_marksGenerationFailed() {
        ChatResponse chatResponse = ChatResponse.builder().aiMessage(AiMessage.from("Nội dung AI")).build();

        stubUntilLlmCall();
        when(llmHelper.call(any())).thenReturn(chatResponse);

        doThrow(new RuntimeException("DB error")).when(generatedContentRepository).save(any());

        assertThatThrownBy(() -> service.generate(1L, request))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GENERATION_RUN_FAILED);

        assertThat(generation.getStatus()).isEqualTo(GenerationStatus.FAILED);
        assertThat(generation.getErrorMessage()).isEqualTo("DB error");
        verify(generationRepository, times(3)).save(generation);
    }

    // Lỗi xảy ra trước khi tạo Generation thì ném nguyên lỗi gốc, không bọc GENERATION_RUN_FAILED
    @Test
    void generate_handlerRejectsInput_doesNotCreateGeneration() {
        when(conversationRepository.findByIdAndStatus(1L, ConversationStatus.ACTIVE)).thenReturn(Optional.of(conversation));
        when(emailHandler.supports(ConversationType.EMAIL_GENERATION)).thenReturn(true);
        when(generationMapper.toJsonNode(request.getInputData())).thenReturn(inputData);
        when(emailHandler.handle(inputData, conversation))
                .thenThrow(new AIConversationException(ErrorCode.EMAIL_GENERATION_PURPOSE_REQUIRED));

        assertThatThrownBy(() -> service.generate(1L, request))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_GENERATION_PURPOSE_REQUIRED);

        verifyNoInteractions(generationRepository, llmHelper);
    }

    // MEETING_MINUTES_GENERATION là loại generation hợp lệ nhưng chưa có handler nào hỗ trợ
    @Test
    void generate_noHandlerForType_throwsHandlerNotFound() {
        conversation.setConversationType(ConversationType.MEETING_MINUTES_GENERATION);
        when(conversationRepository.findByIdAndStatus(1L, ConversationStatus.ACTIVE)).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> service.generate(1L, request))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GENERATION_HANDLER_NOT_FOUND);

        verifyNoInteractions(generationRepository, llmHelper);
    }

    @Test
    void generate_conversationNotFound_throwsConversationNotFound() {
        when(conversationRepository.findByIdAndStatus(1L, ConversationStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generate(1L, request))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONVERSATION_NOT_FOUND);

        verifyNoInteractions(emailHandler, reportHandler, generationRepository);
    }

    @Test
    void generate_noPermission_doesNotLoadConversation() {
        doThrow(new AuthorizationException(ErrorCode.PERMISSION_DENIED))
                .when(currentUserService).requirePermission(Permission.AI_DOCUMENT_GENERATION);

        assertThatThrownBy(() -> service.generate(1L, request))
                .isInstanceOf(AuthorizationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PERMISSION_DENIED);

        verifyNoInteractions(conversationRepository, llmHelper);
    }

    // ===================== getGenerationDetail =====================

    @Test
    void getGenerationDetail_notOwner_throwsAccessDenied() {
        generation.setAiConversation(conversation);

        when(generationRepository.findById(10L)).thenReturn(Optional.of(generation));
        when(currentUserService.getCurrentUserId()).thenReturn(2L);
        doThrow(new AuthorizationException(ErrorCode.ACCESS_DENIED))
                .when(aiConversationHelper).validateOwnership(conversation, 2L);

        assertThatThrownBy(() -> service.getGenerationDetail(10L))
                .isInstanceOf(AuthorizationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);

        verifyNoInteractions(conversationDocumentRepository, aiConversationMapper);
    }

    @Test
    void getGenerationDetail_reportGeneration_includesAttachedDocuments() {
        conversation.setConversationType(ConversationType.REPORT_GENERATION);
        generation.setAiConversation(conversation);
        AIConversationDocument link = AIConversationDocument.builder().id(50L).build();
        ConversationDocumentResponse documentResponse = ConversationDocumentResponse.builder().build();
        GenerationConversationDetailResponse response = GenerationConversationDetailResponse.builder().build();

        when(generationRepository.findById(10L)).thenReturn(Optional.of(generation));
        when(conversationDocumentRepository.findByAiConversationIdWithDocument(1L)).thenReturn(List.of(link));
        when(aiConversationMapper.toConversationDocumentResponse(link)).thenReturn(documentResponse);
        when(conversationDocumentRepository.existsByConversationIdAndDocumentVersionDocumentStatus(1L, DocumentStatus.DELETED))
                .thenReturn(false);
        when(aiConversationMapper.toGenerationDetailResponse(conversation, generation, List.of(documentResponse), false))
                .thenReturn(response);

        assertThat(service.getGenerationDetail(10L)).isSameAs(response);
    }

    @Test
    void getGenerationDetail_notFound_throwsGenerationNotFound() {
        when(generationRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getGenerationDetail(10L))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GENERATION_NOT_FOUND);
    }

    // ===================== test data =====================

    // Stub mọi bước trước lời gọi LLM: tìm conversation, chọn emailHandler, tạo Generation.
    // reportHandler không cần stub supports(): mock trả false nên service bỏ qua nó.
    private void stubUntilLlmCall() {
        when(conversationRepository.findByIdAndStatus(1L, ConversationStatus.ACTIVE)).thenReturn(Optional.of(conversation));
        when(emailHandler.supports(ConversationType.EMAIL_GENERATION)).thenReturn(true);
        when(generationMapper.toJsonNode(request.getInputData())).thenReturn(inputData);
        when(emailHandler.handle(inputData, conversation)).thenReturn(context);
        when(generationMapper.toGeneration(conversation, context, inputData)).thenReturn(generation);
        when(generationRepository.save(generation)).thenReturn(generation);
    }
}
