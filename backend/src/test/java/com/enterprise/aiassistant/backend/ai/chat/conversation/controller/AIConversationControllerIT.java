package com.enterprise.aiassistant.backend.ai.chat.conversation.controller;

import com.enterprise.aiassistant.backend.ai.analytics.usage.entity.AIUsageLog;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.analytics.usage.repository.AIUsageLogRepository;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.chat.conversation.enums.ConversationStatus;
import com.enterprise.aiassistant.backend.ai.chat.conversation.repository.AIConversationRepository;
import com.enterprise.aiassistant.backend.ai.chat.message.entity.AIMessage;
import com.enterprise.aiassistant.backend.ai.chat.message.enums.AIMessageRole;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.entity.Generation;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.enums.GeneratedDocumentType;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.enums.GenerationStatus;
import com.enterprise.aiassistant.backend.department.entity.Department;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.support.IntegrationTestBase;
import com.enterprise.aiassistant.backend.user.entity.User;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AIConversationControllerIT extends IntegrationTestBase {

    private static final String URL = "/api/v1/ai-conversations";

    @Autowired
    private AIConversationRepository conversationRepository;

    @Autowired
    private AIUsageLogRepository usageLogRepository;

    // ===================== POST /document-qa/start =====================

    // Một request chạy cả chuỗi: tạo conversation → gắn tài liệu → gọi AI → lưu tin nhắn.
    // ChatModel giả trả "FAKE ANSWER" nên bước phân loại intent thất bại và rơi về GENERAL_CHAT,
    // vì vậy cần quyền AI_CHAT và không phải gọi Qdrant.
    @Test
    void startDocumentQa_validRequest_createsConversationAttachesDocumentAndAnswers() throws Exception {
        User alice = data.createUser("alice");
        DocumentVersion version = data.createDocument(alice, "hop-dong");

        mockMvc.perform(post(URL + "/document-qa/start")
                        .with(loginAs(alice, Permission.CONVERSATION_CREATE, Permission.AI_DOCUMENT_QA,
                                Permission.CONVERSATION_UPDATE, Permission.DOCUMENT_READ, Permission.AI_CHAT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documentVersionIds": [%d], "content": "Hợp đồng có mấy loại?"}
                                """.formatted(version.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversation.conversationType").value("DOCUMENT_QA"))
                .andExpect(jsonPath("$.message.userMessage.content").value("Hợp đồng có mấy loại?"))
                .andExpect(jsonPath("$.message.assistantMessage.content").value(FAKE_ANSWER));

        assertThat(countRows("ai_conversations")).isEqualTo(1);
        assertThat(countRows("ai_conversation_documents")).isEqualTo(1);
        assertThat(countRows("ai_messages")).isEqualTo(2);
    }

    // Conversation được tạo trước bước gắn tài liệu. Bước gắn lỗi thì transaction rollback,
    // không được để lại conversation rỗng. Unit test dùng mock không chứng minh được điều này.
    @Test
    void startDocumentQa_unreadableDocument_returns403AndCreatesNothing() throws Exception {
        User carol = data.createUser("carol", data.createDepartment("sales"));
        User alice = data.createUser("alice", data.createDepartment("hr"));
        DocumentVersion secret = data.createDocument(carol, "bang-luong");

        mockMvc.perform(post(URL + "/document-qa/start")
                        .with(loginAs(alice, Permission.CONVERSATION_CREATE, Permission.AI_DOCUMENT_QA,
                                Permission.CONVERSATION_UPDATE, Permission.DOCUMENT_READ, Permission.AI_CHAT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documentVersionIds": [%d], "content": "Lương của Carol bao nhiêu?"}
                                """.formatted(secret.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));

        assertThat(countRows("ai_conversations")).isZero();
        assertThat(countRows("ai_messages")).isZero();
    }

    // ===================== POST /generation/start =====================

    @Test
    void startGeneration_emailType_createsConversationAndCompletedGeneration() throws Exception {
        User alice = data.createUser("alice");

        mockMvc.perform(post(URL + "/generation/start")
                        .with(loginAs(alice, Permission.CONVERSATION_CREATE, Permission.AI_DOCUMENT_GENERATION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conversationType": "EMAIL_GENERATION", "inputData": {"purpose": "Xin nghỉ phép"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversation.conversationType").value("EMAIL_GENERATION"))
                .andExpect(jsonPath("$.generation.status").value("COMPLETED"))
                .andExpect(jsonPath("$.generation.generatedContentId", notNullValue()));
    }

    // ===================== PUT /{id} =====================

    @Test
    void renameConversation_owner_updatesTitleInDatabase() throws Exception {
        User alice = data.createUser("alice");
        AIConversation conversation = data.createConversation(alice, ConversationType.DOCUMENT_QA);

        mockMvc.perform(put(URL + "/{id}", conversation.getId())
                        .with(loginAs(alice, Permission.CONVERSATION_UPDATE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Tên mới"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Tên mới"));

        assertThat(conversationRepository.findById(conversation.getId()).orElseThrow().getTitle()).isEqualTo("Tên mới");
    }

    // Bob có đủ quyền tính năng nhưng không phải chủ: ACCESS_DENIED, không phải PERMISSION_DENIED
    @Test
    void renameConversation_otherUser_returns403AndKeepsTitle() throws Exception {
        User alice = data.createUser("alice");
        User bob = data.createUser("bob");
        AIConversation conversation = data.createConversation(alice, ConversationType.DOCUMENT_QA);

        mockMvc.perform(put(URL + "/{id}", conversation.getId())
                        .with(loginAs(bob, Permission.CONVERSATION_UPDATE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Bob đổi tên"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));

        assertThat(conversationRepository.findById(conversation.getId()).orElseThrow().getTitle()).isEqualTo("Hội thoại test");
    }

    // ===================== DELETE /{id} và GET /deleted =====================

    @Test
    void softDeleteConversation_owner_movesConversationToDeletedList() throws Exception {
        User alice = data.createUser("alice");
        AIConversation conversation = data.createConversation(alice, ConversationType.DOCUMENT_QA);

        mockMvc.perform(delete(URL + "/{id}", conversation.getId()).with(loginAs(alice, Permission.CONVERSATION_DELETE)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(URL).with(loginAs(alice, Permission.CONVERSATION_READ)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        mockMvc.perform(get(URL + "/deleted").with(loginAs(alice, Permission.CONVERSATION_READ)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(conversation.getId()))
                .andExpect(jsonPath("$.content[0].status").value("DELETED"))
                .andExpect(jsonPath("$.content[0].deletedAt", notNullValue()));
    }

    // ===================== POST /{id}/restore =====================

    @Test
    void restoreConversation_deletedConversation_becomesActiveAgain() throws Exception {
        User alice = data.createUser("alice");
        AIConversation conversation = data.createDeletedConversation(alice, ConversationType.DOCUMENT_QA);

        mockMvc.perform(post(URL + "/{id}/restore", conversation.getId()).with(loginAs(alice, Permission.CONVERSATION_DELETE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversation.getId()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.deletedAt", nullValue()));

        AIConversation saved = conversationRepository.findById(conversation.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        assertThat(saved.getDeletedAt()).isNull();
    }

    // ===================== DELETE /{id}/hard =====================

    // Bảng con có khóa ngoại tới bảng cha: xóa sai thứ tự thì Postgres báo lỗi.
    // Unit test chỉ kiểm tra thứ tự gọi hàm, test này chứng minh thứ tự đó chạy được trên DB thật.
    @Test
    void hardDeleteConversation_owner_removesConversationAndAllChildRows() throws Exception {
        User alice = data.createUser("alice");
        AIConversation conversation = data.createConversation(alice, ConversationType.DOCUMENT_QA);
        DocumentVersion version = data.createDocument(alice, "hop-dong");
        AIMessage answer = data.createMessage(conversation, AIMessageRole.ASSISTANT, "Có 2 loại");
        data.createMessageSource(answer, data.createChunk(version, "Có 2 loại hợp đồng"));
        data.createMemory(conversation);
        data.attachDocument(conversation, version);
        data.createGeneratedContent(conversation, GeneratedDocumentType.EMAIL, "Email", "Nội dung");

        mockMvc.perform(delete(URL + "/{id}/hard", conversation.getId()).with(loginAs(alice, Permission.CONVERSATION_DELETE)))
                .andExpect(status().isNoContent());

        assertThat(countRows("ai_conversations")).isZero();
        assertThat(countRows("ai_messages")).isZero();
        assertThat(countRows("ai_message_sources")).isZero();
        assertThat(countRows("conversation_memories")).isZero();
        assertThat(countRows("ai_conversation_documents")).isZero();
        assertThat(countRows("generations")).isZero();
        assertThat(countRows("generated_content")).isZero();
        // Tài liệu gốc không thuộc về conversation nên phải còn nguyên
        assertThat(countRows("documents")).isEqualTo(1);
        assertThat(countRows("document_versions")).isEqualTo(1);
        assertThat(countRows("document_chunks")).isEqualTo(1);
    }

    // Usage log là dữ liệu thống kê chi phí: xóa hội thoại không được làm mất số liệu đã dùng
    @Test
    void hardDeleteConversation_owner_keepsUsageLogs() throws Exception {
        User alice = data.createUser("alice");
        AIConversation conversation = data.createConversation(alice, ConversationType.DOCUMENT_QA);
        AIUsageLog log = data.createUsageLog(alice, conversation);

        mockMvc.perform(delete(URL + "/{id}/hard", conversation.getId()).with(loginAs(alice, Permission.CONVERSATION_DELETE)))
                .andExpect(status().isNoContent());

        assertThat(countRows("ai_conversations")).isZero();
        AIUsageLog saved = usageLogRepository.findById(log.getId()).orElseThrow();
        assertThat(saved.getAiConversationId()).isEqualTo(conversation.getId());
        assertThat(saved.getTotalTokens()).isEqualTo(15);
    }

    @Test
    void hardDeleteConversation_otherUser_returns403AndDeletesNothing() throws Exception {
        User alice = data.createUser("alice");
        User bob = data.createUser("bob");
        AIConversation conversation = data.createConversation(alice, ConversationType.DOCUMENT_QA);
        data.createMessage(conversation, AIMessageRole.USER, "Xin chào");

        mockMvc.perform(delete(URL + "/{id}/hard", conversation.getId()).with(loginAs(bob, Permission.CONVERSATION_DELETE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));

        assertThat(countRows("ai_conversations")).isEqualTo(1);
        assertThat(countRows("ai_messages")).isEqualTo(1);
    }

    // ===================== POST /{id}/documents =====================

    @Test
    void attachDocuments_ownDocument_returnsAttachedDocument() throws Exception {
        User alice = data.createUser("alice");
        AIConversation conversation = data.createConversation(alice, ConversationType.DOCUMENT_QA);
        DocumentVersion version = data.createDocument(alice, "hop-dong");

        mockMvc.perform(post(URL + "/{id}/documents", conversation.getId())
                        .with(loginAs(alice, Permission.CONVERSATION_UPDATE, Permission.DOCUMENT_READ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documentVersionIds": [%d]}
                                """.formatted(version.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents", hasSize(1)))
                .andExpect(jsonPath("$.documents[0].documentVersionId").value(version.getId()))
                .andExpect(jsonPath("$.documents[0].documentTitle").value("hop-dong"));
    }

    // DB có unique constraint (conversation, version): gắn lại mà không lọc trước sẽ thành lỗi 500
    @Test
    void attachDocuments_alreadyAttached_keepsSingleLink() throws Exception {
        User alice = data.createUser("alice");
        AIConversation conversation = data.createConversation(alice, ConversationType.DOCUMENT_QA);
        DocumentVersion version = data.createDocument(alice, "hop-dong");
        data.attachDocument(conversation, version);

        mockMvc.perform(post(URL + "/{id}/documents", conversation.getId())
                        .with(loginAs(alice, Permission.CONVERSATION_UPDATE, Permission.DOCUMENT_READ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documentVersionIds": [%d]}
                                """.formatted(version.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents", hasSize(1)));

        assertThat(countRows("ai_conversation_documents")).isEqualTo(1);
    }

    // TODO (bạn tự viết): attachDocuments_unreadableDocument_returns403AndAttachesNothing
    // Bảo mật: Alice không được kéo tài liệu ngoài quyền đọc vào hội thoại của mình.
    // Chuẩn bị:
    //   carol thuộc phòng ban "sales", alice thuộc phòng ban "hr" (data.createUser("carol", data.createDepartment("sales")));
    //   conversation của alice (DOCUMENT_QA); secret = data.createDocument(carol, "bang-luong").
    // Gọi POST URL + "/{id}/documents" với loginAs(alice, Permission.CONVERSATION_UPDATE, Permission.DOCUMENT_READ),
    // body {"documentVersionIds": [id của secret]}
    // → 403, $.error = "ACCESS_DENIED", và countRows("ai_conversation_documents") bằng 0.
    // Gợi ý: phần gọi API giống attachDocuments_ownDocument_returnsAttachedDocument,
    // phần chuẩn bị giống startDocumentQa_unreadableDocument_returns403AndCreatesNothing.
    @Test
    void attachDocuments_unreadableDocument_returns403AndAttachesNothing() throws Exception {
        Department sales =  data.createDepartment("sales");
        Department hr =  data.createDepartment("hr");
        User alice = data.createUser("alice", sales);
        User carol = data.createUser("carol", hr);
        DocumentVersion secret = data.createDocument(carol, "bang-luong");

        AIConversation conversation = data.createConversation(alice, ConversationType.DOCUMENT_QA);

        mockMvc.perform(post(URL + "/{id}/documents", conversation.getId())
                .with(loginAs(alice, Permission.CONVERSATION_UPDATE, Permission.DOCUMENT_READ))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"documentVersionIds": [%d]}
                        """.formatted(secret.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));

        assertThat(countRows("ai_conversation_documents")).isZero();
    }

    // ===================== DELETE /{id}/documents/{versionId} =====================

    @Test
    void removeDocument_attached_returns204AndDeletesOnlyTheLink() throws Exception {
        User alice = data.createUser("alice");
        AIConversation conversation = data.createConversation(alice, ConversationType.DOCUMENT_QA);
        DocumentVersion version = data.createDocument(alice, "hop-dong");
        data.attachDocument(conversation, version);

        mockMvc.perform(delete(URL + "/{id}/documents/{versionId}", conversation.getId(), version.getId())
                        .with(loginAs(alice, Permission.CONVERSATION_UPDATE)))
                .andExpect(status().isNoContent());

        assertThat(countRows("ai_conversation_documents")).isZero();
        assertThat(countRows("document_versions")).isEqualTo(1);
    }

    // ===================== GET /{id} =====================

    // Có 3 tin nhắn, giới hạn 2: phải lấy 2 tin MỚI NHẤT nhưng trả về theo thứ tự cũ → mới để hiển thị
    @Test
    void getDocumentQaConversationDetail_moreMessagesThanLimit_returnsLatestInChronologicalOrder() throws Exception {
        User alice = data.createUser("alice");
        AIConversation conversation = data.createConversation(alice, ConversationType.DOCUMENT_QA);
        data.attachDocument(conversation, data.createDocument(alice, "hop-dong"));
        data.createMessage(conversation, AIMessageRole.USER, "Tin 1");
        data.createMessage(conversation, AIMessageRole.ASSISTANT, "Tin 2");
        data.createMessage(conversation, AIMessageRole.USER, "Tin 3");

        mockMvc.perform(get(URL + "/{id}", conversation.getId())
                        .param("recentMessagesLimit", "2")
                        .with(loginAs(alice, Permission.CONVERSATION_READ)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attachedDocuments", hasSize(1)))
                .andExpect(jsonPath("$.attachedDocuments[0].documentTitle").value("hop-dong"))
                .andExpect(jsonPath("$.recentMessages", hasSize(2)))
                .andExpect(jsonPath("$.recentMessages[0].content").value("Tin 2"))
                .andExpect(jsonPath("$.recentMessages[1].content").value("Tin 3"))
                .andExpect(jsonPath("$.hasMoreMessages").value(true));
    }

    // ===================== GET / =====================

    // Câu query thật: chỉ lấy hội thoại của mình, đang ACTIVE, đúng loại, kèm số tin nhắn
    @Test
    void getConversations_mixedData_returnsOnlyOwnActiveConversationsOfRequestedType() throws Exception {
        User alice = data.createUser("alice");
        User bob = data.createUser("bob");
        AIConversation wanted = data.createConversation(alice, ConversationType.DOCUMENT_QA);
        data.createMessage(wanted, AIMessageRole.USER, "Xin chào");
        data.createMessage(wanted, AIMessageRole.ASSISTANT, "Chào bạn");
        data.createConversation(alice, ConversationType.EMAIL_GENERATION);
        data.createDeletedConversation(alice, ConversationType.DOCUMENT_QA);
        data.createConversation(bob, ConversationType.DOCUMENT_QA);

        mockMvc.perform(get(URL)
                        .param("conversationType", "DOCUMENT_QA")
                        .with(loginAs(alice, Permission.CONVERSATION_READ)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(wanted.getId()))
                .andExpect(jsonPath("$.content[0].messageCount").value(2));
    }

    @Test
    void getConversations_noReadPermission_returns403() throws Exception {
        User alice = data.createUser("alice");

        mockMvc.perform(get(URL).with(loginAs(alice, Permission.AI_CHAT)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("PERMISSION_DENIED"));
    }

    @Test
    void getConversations_notLoggedIn_returns401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
    }

    // ===================== GET /{id}/generation-detail =====================

    @Test
    void getGenerationConversationDetail_multipleRuns_returnsLatestRun() throws Exception {
        User alice = data.createUser("alice");
        AIConversation conversation = data.createConversation(alice, ConversationType.EMAIL_GENERATION);
        data.createGeneration(conversation, GenerationStatus.FAILED);
        Generation latest = data.createGeneration(conversation, GenerationStatus.COMPLETED);

        mockMvc.perform(get(URL + "/{id}/generation-detail", conversation.getId())
                        .with(loginAs(alice, Permission.CONVERSATION_READ)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generationId").value(latest.getId()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                // Email không gắn tài liệu nên trường này là null
                .andExpect(jsonPath("$.attachedDocuments", nullValue()));
    }

    // ===================== GET /{id}/documents và /{id}/generations =====================

    @Test
    void getConversationDocuments_owner_returnsAttachedDocuments() throws Exception {
        User alice = data.createUser("alice");
        AIConversation conversation = data.createConversation(alice, ConversationType.DOCUMENT_QA);
        DocumentVersion version = data.createDocument(alice, "hop-dong");
        data.attachDocument(conversation, version);

        mockMvc.perform(get(URL + "/{id}/documents", conversation.getId())
                        .with(loginAs(alice, Permission.CONVERSATION_READ)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].documentVersionId").value(version.getId()))
                .andExpect(jsonPath("$[0].documentTitle").value("hop-dong"));
    }

    @Test
    void getConversationGenerations_multipleRuns_returnsNewestFirst() throws Exception {
        User alice = data.createUser("alice");
        AIConversation conversation = data.createConversation(alice, ConversationType.EMAIL_GENERATION);
        Generation first = data.createGeneration(conversation, GenerationStatus.FAILED);
        Generation second = data.createGeneration(conversation, GenerationStatus.COMPLETED);

        mockMvc.perform(get(URL + "/{id}/generations", conversation.getId())
                        .with(loginAs(alice, Permission.CONVERSATION_READ)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].generationId").value(second.getId()))
                .andExpect(jsonPath("$.content[1].generationId").value(first.getId()));
    }
}
