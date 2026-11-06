package com.enterprise.aiassistant.backend.ai.knowledge.generation.handler;

import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.infrastructure.prompt.service.PromptBuilderService;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.dto.GenerationContext;
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
class ReportGenerationHandlerTest {

    @Mock
    private PromptBuilderService promptBuilderService;

    // Đọc nội dung tài liệu từ DB nên phải mock
    @Mock
    private DocumentContextService documentContextService;

    @Spy
    private GenerationHelper generationHelper = new GenerationHelper();

    @InjectMocks
    private ReportGenerationHandler handler;

    private final AIConversation conversation = AIConversation.builder().id(1L).build();

    private final ObjectNode inputData = JsonNodeFactory.instance.objectNode().put("title", "Báo cáo quý 3");

    @ParameterizedTest
    @EnumSource(ConversationType.class)
    void supports_eachType_trueOnlyForReportGeneration(ConversationType type) {
        assertThat(handler.supports(type)).isEqualTo(type == ConversationType.REPORT_GENERATION);
    }

    @Test
    void handle_validInput_buildsPromptFromDocumentContext() {
        when(documentContextService.buildContext(1L)).thenReturn("Nội dung tài liệu");
        when(promptBuilderService.buildReportPrompt(any(), eq("Nội dung tài liệu"))).thenReturn("PROMPT");

        GenerationContext context = handler.handle(inputData, conversation);

        assertThat(context.getPrompt()).isEqualTo("PROMPT");
        assertThat(context.getTitle()).isEqualTo("Báo cáo quý 3");
        assertThat(context.getGeneratedType()).isEqualTo(GeneratedType.REPORT);
    }

    // Kiểm tra input trước, chưa hợp lệ thì không tốn công đọc tài liệu
    @Test
    void handle_missingTitle_throwsTitleRequiredBeforeLoadingDocuments() {
        ObjectNode noTitle = JsonNodeFactory.instance.objectNode();

        assertThatThrownBy(() -> handler.handle(noTitle, conversation))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REPORT_GENERATION_TITLE_REQUIRED);

        verifyNoInteractions(documentContextService, promptBuilderService);
    }

    @Test
    void handle_noSourceDocuments_throwsSourceDocumentsRequired() {
        when(documentContextService.buildContext(1L)).thenReturn("");

        assertThatThrownBy(() -> handler.handle(inputData, conversation))
                .isInstanceOf(AIConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GENERATION_SOURCE_DOCUMENTS_REQUIRED);

        verifyNoInteractions(promptBuilderService);
    }
}
