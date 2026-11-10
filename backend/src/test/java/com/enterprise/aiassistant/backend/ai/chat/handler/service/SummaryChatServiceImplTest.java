package com.enterprise.aiassistant.backend.ai.chat.handler.service;

import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.chat.conversation.helper.AIConversationHelper;
import com.enterprise.aiassistant.backend.ai.chat.handler.helper.HandlerHelper;
import com.enterprise.aiassistant.backend.ai.chat.message.helper.AIMessageHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.llm.helper.LLMHelper;
import com.enterprise.aiassistant.backend.ai.infrastructure.prompt.service.PromptBuilderService;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.SummaryGenerationInput;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.helper.GenerationHelper;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.service.DocumentContextService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummaryChatServiceImplTest {

    @Mock
    private DocumentContextService documentContextService;

    @Mock
    private PromptBuilderService promptBuilderService;

    @Mock
    private ChatModel chatModel;

    @Mock
    private LLMHelper llmHelper;

    @Spy
    private GenerationHelper generationHelper = new GenerationHelper();

    @Mock
    private AIConversationHelper aiConversationHelper;

    @Spy
    private AIMessageHelper messageHelper = new AIMessageHelper();

    @Mock
    private HandlerHelper handlerHelper;

    @InjectMocks
    private SummaryChatServiceImpl service;

    private final AIConversation conversation = AIConversation.builder().id(1L).build();

    @Test
    void summarize_documentsAttached_returnsLlmSummary() {
        SummaryGenerationInput input = new SummaryGenerationInput();
        ChatResponse chatResponse = ChatResponse.builder().aiMessage(AiMessage.from("Tài liệu nói về A")).build();

        when(documentContextService.buildContext(1L)).thenReturn("Nội dung tài liệu");
        when(handlerHelper.toSummaryInput("Tóm tắt giúp tôi")).thenReturn(input);
        // Prompt phải được dựng từ đúng nội dung tài liệu của conversation
        when(promptBuilderService.buildSummaryPrompt(input, "Nội dung tài liệu")).thenReturn("PROMPT");
        when(llmHelper.call(any())).thenReturn(chatResponse);

        assertThat(service.summarize(conversation, "Tóm tắt giúp tôi")).isEqualTo("Tài liệu nói về A");
    }

    @Test
    void summarize_noDocuments_throwsSourceDocumentsRequired() {
        when(documentContextService.buildContext(1L)).thenReturn("   ");

        assertThatThrownBy(() -> service.summarize(conversation, "Tóm tắt giúp tôi"))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GENERATION_SOURCE_DOCUMENTS_REQUIRED);

        verifyNoInteractions(promptBuilderService, llmHelper);
    }
}
