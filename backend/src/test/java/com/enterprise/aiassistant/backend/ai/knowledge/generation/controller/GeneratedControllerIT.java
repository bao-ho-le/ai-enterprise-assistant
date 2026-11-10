package com.enterprise.aiassistant.backend.ai.knowledge.generation.controller;

import com.enterprise.aiassistant.backend.ai.knowledge.generation.entity.GeneratedContent;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.enums.GeneratedDocumentType;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.repository.GeneratedContentRepository;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.support.IntegrationTestBase;
import com.enterprise.aiassistant.backend.user.entity.User;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Lưu ý: controller này hard-code "/api/v1/generated-contents", không dùng ${api.prefix}
class GeneratedControllerIT extends IntegrationTestBase {

    private static final String URL = "/api/v1/generated-contents";

    @Autowired
    private GeneratedContentRepository generatedContentRepository;

    // ===================== GET /generated-contents =====================

    // Bảo mật: nội dung thuộc về chủ của conversation đã sinh ra nó, người khác không thấy
    @Test
    void getGeneratedContents_owner_returnsOnlyOwnContents() throws Exception {
        User alice = data.createUser("alice");
        User bob = data.createUser("bob");
        data.createGeneratedContent(alice, GeneratedDocumentType.EMAIL, "Email của Alice", "Nội dung A");
        data.createGeneratedContent(bob, GeneratedDocumentType.EMAIL, "Email của Bob", "Nội dung B");

        mockMvc.perform(get(URL).with(loginAs(alice, Permission.AI_DOCUMENT_GENERATION)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Email của Alice"));
    }

    @Test
    void getGeneratedContents_filterByType_returnsOnlyThatType() throws Exception {
        User alice = data.createUser("alice");
        data.createGeneratedContent(alice, GeneratedDocumentType.EMAIL, "Email", "Nội dung");
        data.createGeneratedContent(alice, GeneratedDocumentType.REPORT, "Báo cáo", "Nội dung");

        mockMvc.perform(get(URL)
                        .param("generatedType", "REPORT")
                        .with(loginAs(alice, Permission.AI_DOCUMENT_GENERATION)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].generatedType").value("REPORT"));
    }

    @Test
    void getGeneratedContents_noGenerationPermission_returns403() throws Exception {
        User alice = data.createUser("alice");

        mockMvc.perform(get(URL).with(loginAs(alice, Permission.AI_CHAT)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("PERMISSION_DENIED"));
    }

    @Test
    void getGeneratedContents_notLoggedIn_returns401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
    }

    // ===================== GET /generated-contents/{id} =====================

    @Test
    void getGeneratedContentById_owner_returnsFullContent() throws Exception {
        User alice = data.createUser("alice");
        GeneratedContent content = data.createGeneratedContent(alice, GeneratedDocumentType.EMAIL, "Xin nghỉ phép", "Kính gửi anh...");

        mockMvc.perform(get(URL + "/{id}", content.getId()).with(loginAs(alice, Permission.AI_DOCUMENT_GENERATION)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(content.getId()))
                .andExpect(jsonPath("$.title").value("Xin nghỉ phép"))
                .andExpect(jsonPath("$.content").value("Kính gửi anh..."));
    }

    @Test
    void getGeneratedContentById_notFound_returns404() throws Exception {
        User alice = data.createUser("alice");

        mockMvc.perform(get(URL + "/{id}", 999_999).with(loginAs(alice, Permission.AI_DOCUMENT_GENERATION)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("GENERATED_CONTENT_NOT_FOUND"));
    }

    // TODO (bạn tự viết): getGeneratedContentById_otherUser_returns403
    // Bảo mật: bob đã đăng nhập, có đủ quyền AI_DOCUMENT_GENERATION nhưng xem nội dung của alice → 403.
    // Chuẩn bị: alice, bob = data.createUser(...); content = data.createGeneratedContent(alice, ...).
    // Gọi GET URL + "/{id}" với loginAs(bob, Permission.AI_DOCUMENT_GENERATION)
    // → status().isForbidden() và $.error = "ACCESS_DENIED" (không phải PERMISSION_DENIED:
    //   bob CÓ quyền tính năng, chỉ là không phải chủ của nội dung này).
    // Gợi ý: làm giống getGeneratedContentById_owner_returnsFullContent, đổi người gọi.
    @Test
    void getGeneratedContentById_otherUser_returns403() throws Exception {
        User alice = data.createUser("alice");
        User bob = data.createUser("bob");
        GeneratedContent content = data.createGeneratedContent(alice, GeneratedDocumentType.EMAIL, "Email của Alice", "Nội dung A");
        data.createGeneratedContent(bob, GeneratedDocumentType.EMAIL, "Email của Bob", "Nội dung B");

        mockMvc.perform(get(URL + "/{id}", content.getId()).with(loginAs(bob, Permission.AI_DOCUMENT_GENERATION)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value(ErrorCode.ACCESS_DENIED.name()));
    }

    // ===================== PUT /generated-contents/{id} =====================

    // Kiểm tra cả response lẫn database: service không gọi save() mà dựa vào Hibernate tự lưu thay đổi
    // khi transaction commit. Chỉ integration test mới chứng minh được dữ liệu thật sự xuống DB.
    @Test
    void updateGeneratedContent_owner_savesTrimmedContentToDatabase() throws Exception {
        User alice = data.createUser("alice");
        GeneratedContent content = data.createGeneratedContent(alice, GeneratedDocumentType.EMAIL, "Tiêu đề cũ", "Nội dung cũ");

        mockMvc.perform(put(URL + "/{id}", content.getId())
                        .with(loginAs(alice, Permission.AI_DOCUMENT_GENERATION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "  Tiêu đề mới  ", "content": "  Nội dung mới  "}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Tiêu đề mới"))
                .andExpect(jsonPath("$.content").value("Nội dung mới"));

        GeneratedContent saved = generatedContentRepository.findById(content.getId()).orElseThrow();
        assertThat(saved.getTitle()).isEqualTo("Tiêu đề mới");
        assertThat(saved.getContent()).isEqualTo("Nội dung mới");
    }

    // Bị @NotBlank trong UpdateGeneratedContentRequest chặn ở @Valid, service không được chạy
    @Test
    void updateGeneratedContent_blankTitle_returns400AndKeepsOldContent() throws Exception {
        User alice = data.createUser("alice");
        GeneratedContent content = data.createGeneratedContent(alice, GeneratedDocumentType.EMAIL, "Tiêu đề cũ", "Nội dung cũ");

        mockMvc.perform(put(URL + "/{id}", content.getId())
                        .with(loginAs(alice, Permission.AI_DOCUMENT_GENERATION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "   ", "content": "Nội dung mới"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        GeneratedContent saved = generatedContentRepository.findById(content.getId()).orElseThrow();
        assertThat(saved.getTitle()).isEqualTo("Tiêu đề cũ");
        assertThat(saved.getContent()).isEqualTo("Nội dung cũ");
    }
}
