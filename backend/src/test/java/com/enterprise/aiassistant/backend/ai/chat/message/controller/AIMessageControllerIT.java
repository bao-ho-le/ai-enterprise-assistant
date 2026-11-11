package com.enterprise.aiassistant.backend.ai.chat.message.controller;

import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.analytics.usage.repository.AIUsageLogRepository;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.chat.message.entity.AIMessage;
import com.enterprise.aiassistant.backend.ai.chat.message.enums.AIMessageRole;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.support.IntegrationTestBase;
import com.enterprise.aiassistant.backend.user.entity.User;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AIMessageControllerIT extends IntegrationTestBase {

    private static final String URL = "/api/v1/ai-conversations/{conversationId}/messages";

    @Autowired
    private AIUsageLogRepository usageLogRepository;

    // ===================== POST /messages =====================

    // ChatModel giả trả "FAKE ANSWER" nên phân loại intent thất bại và rơi về GENERAL_CHAT
    @Test
    void sendMessage_generalChat_savesBothMessagesAndUpdatesMemory() throws Exception {
        User alice = data.createUser("alice");
        AIConversation conversation = data.createConversation(alice, ConversationType.DOCUMENT_QA);

        mockMvc.perform(post(URL, conversation.getId())
                        .with(loginAs(alice, Permission.AI_CHAT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "  Xin chào  "}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userMessage.role").value("USER"))
                .andExpect(jsonPath("$.userMessage.content").value("Xin chào"))
                .andExpect(jsonPath("$.assistantMessage.role").value("ASSISTANT"))
                .andExpect(jsonPath("$.assistantMessage.content").value(FAKE_ANSWER));

        assertThat(countRows("ai_messages")).isEqualTo(2);
        assertThat(countRows("conversation_memories")).isEqualTo(1);
    }

    // Luồng thật: AIUsageContext → AIUsageListener → event → lưu log sau khi transaction commit.
    // Một tin nhắn gọi AI 2 lần (phân loại intent và trả lời) nên có 2 dòng log.
    @Test
    void sendMessage_generalChat_recordsUsageLogsForConversation() throws Exception {
        User alice = data.createUser("alice");
        AIConversation conversation = data.createConversation(alice, ConversationType.DOCUMENT_QA);

        mockMvc.perform(post(URL, conversation.getId())
                        .with(loginAs(alice, Permission.AI_CHAT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "Xin chào"}
                                """))
                .andExpect(status().isOk());

        assertThat(usageLogRepository.findAll()).hasSize(2).allSatisfy(log -> {
            assertThat(log.getAiConversationId()).isEqualTo(conversation.getId());
            assertThat(log.getUserId()).isEqualTo(alice.getId());
            assertThat(log.getModel()).isEqualTo("fake-model");
            assertThat(log.getTotalTokens()).isEqualTo(15);
            assertThat(log.getStatus()).isEqualTo(AIUsageStatus.SUCCESS);
        });
    }

    // Tin nhắn user được save trước bước kiểm tra quyền, nhưng sendMessage chạy trong một transaction:
    // có lỗi là rollback, nên DB không còn tin nhắn nào.
    @Test
    void sendMessage_missingAiChatPermission_returns403AndRollsBackUserMessage() throws Exception {
        User alice = data.createUser("alice");
        AIConversation conversation = data.createConversation(alice, ConversationType.DOCUMENT_QA);

        mockMvc.perform(post(URL, conversation.getId())
                        .with(loginAs(alice, Permission.AI_DOCUMENT_QA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "Xin chào"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("PERMISSION_DENIED"));

        assertThat(countRows("ai_messages")).isZero();
        assertThat(countRows("conversation_memories")).isZero();
    }

    @Test
    void sendMessage_blankContent_returns400() throws Exception {
        User alice = data.createUser("alice");
        AIConversation conversation = data.createConversation(alice, ConversationType.DOCUMENT_QA);

        mockMvc.perform(post(URL, conversation.getId())
                        .with(loginAs(alice, Permission.AI_CHAT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        assertThat(countRows("ai_messages")).isZero();
    }

    // TODO (bạn tự viết): sendMessage_otherUsersConversation_returns403AndSavesNothing
    // Bảo mật: bob có quyền AI_CHAT nhưng gửi tin vào hội thoại của alice.
    // Chuẩn bị: alice, bob = data.createUser(...); conversation của alice (DOCUMENT_QA).
    // Gọi POST URL với loginAs(bob, Permission.AI_CHAT), body {"content": "Xin chào"}
    // → 403, $.error = "ACCESS_DENIED", và countRows("ai_messages") bằng 0.
    // Gợi ý: làm giống sendMessage_missingAiChatPermission_returns403AndRollsBackUserMessage,
    // đổi người gọi và mã lỗi.
    @Test
    void sendMessage_otherUsersConversation_returns403AndSavesNothing() throws Exception {
        User alice = data.createUser("alice");
        User bob = data.createUser("bob");
        AIConversation conversation = data.createConversation(alice, ConversationType.DOCUMENT_QA);

        mockMvc.perform(post(URL, conversation.getId())
                .with(loginAs(bob, Permission.AI_CHAT))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"content": "Xin chào"}
                        """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));

        assertThat(countRows("ai_messages")).isZero();
    }

    // ===================== GET /messages =====================

    // Tải tin cũ hơn theo mốc beforeId: mỗi trang vẫn hiển thị theo thứ tự cũ → mới
    @Test
    void getMessages_givenBeforeId_returnsOlderMessagesInChronologicalOrder() throws Exception {
        User alice = data.createUser("alice");
        AIConversation conversation = data.createConversation(alice, ConversationType.DOCUMENT_QA);
        data.createMessage(conversation, AIMessageRole.USER, "Tin 1");
        data.createMessage(conversation, AIMessageRole.ASSISTANT, "Tin 2");
        AIMessage third = data.createMessage(conversation, AIMessageRole.USER, "Tin 3");

        mockMvc.perform(get(URL, conversation.getId())
                        .param("beforeId", third.getId().toString())
                        .param("size", "1")
                        .with(loginAs(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].content").value("Tin 2"))
                .andExpect(jsonPath("$.hasMore").value(true));
    }

    @Test
    void getMessages_otherUsersConversation_returns403() throws Exception {
        User alice = data.createUser("alice");
        User bob = data.createUser("bob");
        AIConversation conversation = data.createConversation(alice, ConversationType.DOCUMENT_QA);

        mockMvc.perform(get(URL, conversation.getId()).with(loginAs(bob)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }

    // ===================== GET /messages/{id} =====================

    @Test
    void getMessageEvidence_answerWithSource_returnsChunkAndDocumentTitle() throws Exception {
        User alice = data.createUser("alice");
        AIConversation conversation = data.createConversation(alice, ConversationType.DOCUMENT_QA);
        DocumentVersion version = data.createDocument(alice, "hop-dong");
        AIMessage answer = data.createMessage(conversation, AIMessageRole.ASSISTANT, "Có 2 loại");
        data.createMessageSource(answer, data.createChunk(version, "Có 2 loại hợp đồng lao động"));

        mockMvc.perform(get(URL + "/{messageId}", conversation.getId(), answer.getId()).with(loginAs(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Có 2 loại"))
                .andExpect(jsonPath("$.sources", hasSize(1)))
                .andExpect(jsonPath("$.sources[0].documentTitle").value("hop-dong"))
                .andExpect(jsonPath("$.sources[0].content").value("Có 2 loại hợp đồng lao động"))
                .andExpect(jsonPath("$.sources[0].pageNumber").value(3));
    }

    // Bảo mật: lấy id tin nhắn của hội thoại khác rồi ghép vào hội thoại của mình cũng không xem được
    @Test
    void getMessageEvidence_messageFromAnotherConversation_returns404() throws Exception {
        User alice = data.createUser("alice");
        User bob = data.createUser("bob");
        AIConversation aliceConversation = data.createConversation(alice, ConversationType.DOCUMENT_QA);
        AIConversation bobConversation = data.createConversation(bob, ConversationType.DOCUMENT_QA);
        AIMessage bobMessage = data.createMessage(bobConversation, AIMessageRole.USER, "Bí mật của Bob");

        mockMvc.perform(get(URL + "/{messageId}", aliceConversation.getId(), bobMessage.getId()).with(loginAs(alice)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("MESSAGE_NOT_FOUND"));
    }
}
