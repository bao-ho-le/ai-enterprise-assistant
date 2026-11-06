package com.enterprise.aiassistant.backend.ai.knowledge.generation.handler;

import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.infrastructure.prompt.service.PromptBuilderService;
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
class FormGenerationHandlerTest {

    @Mock
    private PromptBuilderService promptBuilderService;

    @Spy
    private GenerationHelper generationHelper = new GenerationHelper();

    @InjectMocks
    private FormGenerationHandler handler;

    private final AIConversation conversation = AIConversation.builder().id(1L).build();

    @ParameterizedTest
    @EnumSource(ConversationType.class)
    void supports_eachType_trueOnlyForFormGeneration(ConversationType type) {
        assertThat(handler.supports(type)).isEqualTo(type == ConversationType.FORM_GENERATION);
    }

    @Test
    void handle_validInput_buildsFormContext() {
        ObjectNode inputData = JsonNodeFactory.instance.objectNode()
                .put("purpose", "Đơn xin nghỉ phép")
                .put("fields", "Họ tên, Ngày nghỉ");
        when(promptBuilderService.buildFormPrompt(any())).thenReturn("PROMPT");

        GenerationContext context = handler.handle(inputData, conversation);

        assertThat(context.getPrompt()).isEqualTo("PROMPT");
        assertThat(context.getTitle()).isEqualTo("Đơn xin nghỉ phép");
        assertThat(context.getGeneratedType()).isEqualTo(GeneratedType.FORM);
    }

    @Test
    void handle_missingPurpose_throwsPurposeRequired() {
        ObjectNode noPurpose = JsonNodeFactory.instance.objectNode().put("fields", "Họ tên");

        assertThatThrownBy(() -> handler.handle(noPurpose, conversation))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORM_GENERATION_PURPOSE_REQUIRED);

        verifyNoInteractions(promptBuilderService);
    }
}
