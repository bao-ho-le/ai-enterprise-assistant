package com.enterprise.aiassistant.backend.ai.knowledge.generation.handler;

import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.infrastructure.prompt.service.PromptBuilderService;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.EmailGenerationInput;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.GenerationContext;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.enums.GeneratedType;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.helper.GenerationHelper;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AIConversationException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Không mock tất cả: GenerationHelper chỉ xử lý dữ liệu (không DB, không LLM) nên dùng object thật,
// nhờ vậy test đưa vào JSON giống lúc chạy thật. PromptBuilderService đã có test riêng nên mock lại.
@ExtendWith(MockitoExtension.class)
class EmailGenerationHandlerTest {

    @Mock
    private PromptBuilderService promptBuilderService;

    @Spy
    private GenerationHelper generationHelper = new GenerationHelper();

    @InjectMocks
    private EmailGenerationHandler handler;

    private final AIConversation conversation = AIConversation.builder().id(1L).build();

    // Chạy với mọi giá trị của enum: chỉ EMAIL_GENERATION được trả true
    @ParameterizedTest
    @EnumSource(ConversationType.class)
    void supports_eachType_trueOnlyForEmailGeneration(ConversationType type) {
        assertThat(handler.supports(type)).isEqualTo(type == ConversationType.EMAIL_GENERATION);
    }

    @Test
    void handle_validInput_buildsEmailContext() {
        ObjectNode inputData = JsonNodeFactory.instance.objectNode()
                .put("purpose", "  Xin nghỉ phép  ")
                .put("recipient", "Phòng nhân sự");
        when(promptBuilderService.buildEmailPrompt(any())).thenReturn("PROMPT");

        GenerationContext context = handler.handle(inputData, conversation);

        assertThat(context.getPrompt()).isEqualTo("PROMPT");
        assertThat(context.getTitle()).isEqualTo("Xin nghỉ phép");
        assertThat(context.getGeneratedType()).isEqualTo(GeneratedType.EMAIL);

        // JSON phải được đọc thành EmailGenerationInput đúng trước khi đưa cho prompt builder
        ArgumentCaptor<EmailGenerationInput> captor = ArgumentCaptor.forClass(EmailGenerationInput.class);
        verify(promptBuilderService).buildEmailPrompt(captor.capture());
        assertThat(captor.getValue().getRecipient()).isEqualTo("Phòng nhân sự");
    }

    @Test
    void handle_longPurpose_truncatesTitleTo120Characters() {
        ObjectNode inputData = JsonNodeFactory.instance.objectNode().put("purpose", "a".repeat(200));

        assertThat(handler.handle(inputData, conversation).getTitle()).hasSize(120);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void handle_blankPurpose_throwsPurposeRequired(String purpose) {
        ObjectNode inputData = JsonNodeFactory.instance.objectNode().put("purpose", purpose);

        assertThatThrownBy(() -> handler.handle(inputData, conversation))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_GENERATION_PURPOSE_REQUIRED);

        verifyNoInteractions(promptBuilderService);
    }
}
