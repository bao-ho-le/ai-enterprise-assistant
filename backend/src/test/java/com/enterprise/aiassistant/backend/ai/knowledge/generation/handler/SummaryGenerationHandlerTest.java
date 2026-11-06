package com.enterprise.aiassistant.backend.ai.knowledge.generation.handler;

import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.infrastructure.prompt.service.PromptBuilderService;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.GenerationContext;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.SummaryGenerationInput;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.enums.GeneratedType;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.helper.GenerationHelper;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.service.DocumentContextService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummaryGenerationHandlerTest {

    @Mock
    private PromptBuilderService promptBuilderService;

    @Mock
    private DocumentContextService documentContextService;

    @Spy
    private GenerationHelper generationHelper = new GenerationHelper();

    @InjectMocks
    private SummaryGenerationHandler handler;

    private final AIConversation conversation = AIConversation.builder().id(1L).build();

    private final ObjectNode inputData = JsonNodeFactory.instance.objectNode().put("style", "BULLET_POINTS");

    @ParameterizedTest
    @EnumSource(ConversationType.class)
    void supports_eachType_trueOnlyForSummaryGeneration(ConversationType type) {
        assertThat(handler.supports(type)).isEqualTo(type == ConversationType.SUMMARY_GENERATION);
    }

    @Test
    void handle_validInput_titleIncludesStyle() {
        when(documentContextService.buildContext(1L)).thenReturn("Nội dung tài liệu");
        when(promptBuilderService.buildSummaryPrompt(any(), eq("Nội dung tài liệu"))).thenReturn("PROMPT");

        GenerationContext context = handler.handle(inputData, conversation);

        assertThat(context.getPrompt()).isEqualTo("PROMPT");
        assertThat(context.getTitle()).isEqualTo("Summary - BULLET_POINTS");
        assertThat(context.getGeneratedType()).isEqualTo(GeneratedType.SUMMARY);
    }

    @Test
    void handle_missingStyle_throwsStyleRequired() {
        ObjectNode noStyle = JsonNodeFactory.instance.objectNode();

        assertThatThrownBy(() -> handler.handle(noStyle, conversation))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SUMMARY_GENERATION_STYLE_REQUIRED);

        verifyNoInteractions(documentContextService, promptBuilderService);
    }

    // TODO (bạn tự viết): handle_noSourceDocuments_throwsSourceDocumentsRequired
    // Input hợp lệ (biến inputData), nhưng conversation chưa gắn tài liệu nào:
    // documentContextService.buildContext(1L) trả chuỗi trắng "   "
    // → ném AIConversationException với ErrorCode.GENERATION_SOURCE_DOCUMENTS_REQUIRED,
    //   và prompt builder không được gọi (verifyNoInteractions(promptBuilderService)).
    // Gợi ý: ReportGenerationHandlerTest có test cùng tên, cách viết y hệt.
    @Test
    void handle_noSourceDocuments_throwsSourceDocumentsRequired() {

        when(documentContextService.buildContext(1L)).thenReturn("   ");

        assertThatThrownBy(() -> handler.handle(inputData, conversation))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GENERATION_SOURCE_DOCUMENTS_REQUIRED);

        verifyNoInteractions(promptBuilderService);
    }

}
