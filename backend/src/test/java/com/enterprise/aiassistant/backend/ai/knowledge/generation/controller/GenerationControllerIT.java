package com.enterprise.aiassistant.backend.ai.knowledge.generation.controller;

import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.entity.GeneratedContent;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.entity.Generation;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.enums.GenerationStatus;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.repository.GeneratedContentRepository;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.repository.GenerationRepository;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.support.IntegrationTestBase;
import com.enterprise.aiassistant.backend.user.entity.User;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GenerationControllerIT extends IntegrationTestBase {

    private static final String GENERATE_URL = "/api/v1/ai-conversations/{conversationId}/generate";
    private static final String DETAIL_URL = "/api/v1/ai-conversations/generations/{generationId}";

    private static final String EMAIL_BODY = """
            {"inputData": {"purpose": "Xin nghỉ phép"}}
            """;

    @Autowired
    private GenerationRepository generationRepository;

    @Autowired
    private GeneratedContentRepository generatedContentRepository;

    // ===================== POST /generate =====================

    @Test
    void generate_emailConversation_savesContentAndMarksCompleted() throws Exception {
        User alice = data.createUser("alice");
        AIConversation conversation = data.createConversation(alice, ConversationType.EMAIL_GENERATION);

        mockMvc.perform(post(GENERATE_URL, conversation.getId())
                        .with(loginAs(alice, Permission.AI_DOCUMENT_GENERATION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMAIL_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        Generation generation = generationRepository.findAll().get(0);
        assertThat(generation.getStatus()).isEqualTo(GenerationStatus.COMPLETED);
        // Cột input_data kiểu jsonb: kiểm tra lưu và đọc lại đúng trên Postgres thật
        assertThat(generation.getInputData().get("purpose").asText()).isEqualTo("Xin nghỉ phép");

        List<GeneratedContent> contents = generatedContentRepository.findAll();
        assertThat(contents).singleElement().satisfies(content -> {
            assertThat(content.getContent()).isEqualTo(FAKE_ANSWER);
            assertThat(content.getTitle()).isEqualTo("Xin nghỉ phép");
        });
    }

    // Report đọc toàn văn tài liệu đính kèm từ DB (DocumentContextService) rồi đưa vào prompt gửi AI
    @Test
    void generate_reportWithAttachedDocument_putsDocumentTextIntoPrompt() throws Exception {
        User alice = data.createUser("alice");
        AIConversation conversation = data.createConversation(alice, ConversationType.REPORT_GENERATION);
        DocumentVersion version = data.createDocument(alice, "doanh-thu");
        data.createDocumentText(version, "Doanh thu quý 3 tăng 20%");
        data.attachDocument(conversation, version);

        mockMvc.perform(post(GENERATE_URL, conversation.getId())
                        .with(loginAs(alice, Permission.AI_DOCUMENT_GENERATION, Permission.DOCUMENT_READ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inputData": {"title": "Báo cáo quý 3"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        assertThat(fakeChatModel.lastPrompt())
                .contains("doanh-thu")
                .contains("Doanh thu quý 3 tăng 20%");
    }

    @Test
    void generate_reportWithoutDocuments_returns400AndCreatesNoGeneration() throws Exception {
        User alice = data.createUser("alice");
        AIConversation conversation = data.createConversation(alice, ConversationType.REPORT_GENERATION);

        mockMvc.perform(post(GENERATE_URL, conversation.getId())
                        .with(loginAs(alice, Permission.AI_DOCUMENT_GENERATION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inputData": {"title": "Báo cáo quý 3"}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("GENERATION_SOURCE_DOCUMENTS_REQUIRED"));

        assertThat(countRows("generations")).isZero();
    }

    @Test
    void generate_noGenerationPermission_returns403() throws Exception {
        User alice = data.createUser("alice");
        AIConversation conversation = data.createConversation(alice, ConversationType.EMAIL_GENERATION);

        mockMvc.perform(post(GENERATE_URL, conversation.getId())
                        .with(loginAs(alice, Permission.AI_CHAT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMAIL_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("PERMISSION_DENIED"));
    }

    @Test
    void generate_otherUsersConversation_returns403() throws Exception {
        User alice = data.createUser("alice");
        User bob = data.createUser("bob");
        AIConversation conversation = data.createConversation(alice, ConversationType.EMAIL_GENERATION);

        mockMvc.perform(post(GENERATE_URL, conversation.getId())
                        .with(loginAs(bob, Permission.AI_DOCUMENT_GENERATION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMAIL_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }

    @Test
    void generate_llmFails_returns500AndSavesNoContent() throws Exception {
        User alice = data.createUser("alice");
        AIConversation conversation = data.createConversation(alice, ConversationType.EMAIL_GENERATION);
        fakeChatModel.failWith(new RuntimeException("Gemini quá tải"));

        mockMvc.perform(post(GENERATE_URL, conversation.getId())
                        .with(loginAs(alice, Permission.AI_DOCUMENT_GENERATION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMAIL_BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("GENERATION_RUN_FAILED"));

        assertThat(countRows("generated_content")).isZero();
    }

    // Lần chạy lỗi phải được lưu lại với trạng thái FAILED. Trước đây generate() rollback cả transaction
    // khi ném exception nên mất dòng này; unit test không phát hiện được vì repository là mock.
    @Test
    void generate_llmFails_keepsFailedRunInHistory() throws Exception {
        User alice = data.createUser("alice");
        AIConversation conversation = data.createConversation(alice, ConversationType.EMAIL_GENERATION);
        fakeChatModel.failWith(new RuntimeException("Gemini quá tải"));

        mockMvc.perform(post(GENERATE_URL, conversation.getId())
                        .with(loginAs(alice, Permission.AI_DOCUMENT_GENERATION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMAIL_BODY))
                .andExpect(status().isInternalServerError());

        assertThat(generationRepository.findAll()).singleElement().satisfies(generation -> {
            assertThat(generation.getStatus()).isEqualTo(GenerationStatus.FAILED);
            assertThat(generation.getErrorMessage()).isNotBlank();
        });
    }

    // ===================== GET /generations/{id} =====================

    @Test
    void getGenerationDetail_owner_returnsRunStatus() throws Exception {
        User alice = data.createUser("alice");
        AIConversation conversation = data.createConversation(alice, ConversationType.EMAIL_GENERATION);
        Generation generation = data.createGeneration(conversation, GenerationStatus.COMPLETED);

        mockMvc.perform(get(DETAIL_URL, generation.getId()).with(loginAs(alice, Permission.AI_DOCUMENT_GENERATION)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generationId").value(generation.getId()))
                .andExpect(jsonPath("$.conversationId").value(conversation.getId()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void getGenerationDetail_otherUser_returns403() throws Exception {
        User alice = data.createUser("alice");
        User bob = data.createUser("bob");
        Generation generation = data.createGeneration(
                data.createConversation(alice, ConversationType.EMAIL_GENERATION), GenerationStatus.COMPLETED);

        mockMvc.perform(get(DETAIL_URL, generation.getId()).with(loginAs(bob, Permission.AI_DOCUMENT_GENERATION)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }
}
